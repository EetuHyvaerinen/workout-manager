package dev.rezu.dashboard;

import dev.rezu.database.DatabasePoolMetrics;
import dev.rezu.logger.AsyncLogger;
import dev.rezu.database.DatabaseConnectionManager;
import dev.rezu.database.PooledConnection;
import dev.rezu.logger.LogLevel;

import java.io.*;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.StructuredTaskScope;
import java.util.stream.Stream;

public class DashboardService {

    private static final AsyncLogger logger = AsyncLogger.getLogger(DashboardService.class);
    private static final String LOG_DIR = "logs";
    private final DatabaseConnectionManager connectionManager;
    private final DashboardDAO dashboardDAO;
    private final Instant serverStartTime;
    
    public DashboardService(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.dashboardDAO = new DashboardDAO();
        this.serverStartTime = Instant.now();
    }

    public SystemStats getSystemStats() {
        try (PooledConnection conn = connectionManager.getConnection()) {
            try (var scope = StructuredTaskScope.open()) {
                var databaseCountsFuture = scope.fork(() -> dashboardDAO.getAggregatedDbCounts(conn));
                var databaseStatsFuture = scope.fork(() -> getDatabaseStats());
                var serverStatsFuture = scope.fork(() -> getServerStats());

                scope.join();

                DashboardDAO.DbCounts counts  = databaseCountsFuture.get();
                DatabasePoolMetrics dbStats   = databaseStatsFuture.get();
                ServerStats serverStats       = serverStatsFuture.get();

                return new SystemStats(
                        counts.totalUsers(),
                        counts.totalWorkouts(),
                        counts.totalExercises(),
                        counts.activeUsers24h(),
                        counts.workoutsToday(),
                        dbStats,
                        serverStats,
                        Instant.now()
                );
            } catch (InterruptedException e) {
                logger.error("Failed to collect system stats", e);
                throw new RuntimeException("Database error during stats collection", e);
            }

        } catch (SQLException e) {
            logger.error("Failed to collect system stats", e);
            throw new RuntimeException("Database error during stats collection", e);
        }
    }

    private DatabasePoolMetrics getDatabaseStats() {
        DatabasePoolMetrics metrics = connectionManager.getPoolMetrics();
        return metrics;
    }

    private ServerStats getServerStats() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory() / (1024 * 1024); // MB
        long freeMemory = runtime.freeMemory() / (1024 * 1024);   // MB
        long usedMemory = totalMemory - freeMemory;
        
        long uptimeSeconds = Duration.between(serverStartTime, Instant.now()).getSeconds();

        return new ServerStats(
            uptimeSeconds,
            totalMemory,
            usedMemory,
            freeMemory,
            runtime.availableProcessors(),
            Thread.activeCount(),
                AsyncLogger.getQueueSize(),
                AsyncLogger.getQueueCapacity(),
                AsyncLogger.getDroppedCount()
        );
    }

    public LogResponse getLogs(String loggerName, LogLevel levelFilter, int maxLines) {
        List<String> activeLoggers = AsyncLogger.getActiveLoggerNames();
        Instant now = Instant.now();

        if (!activeLoggers.contains(loggerName)) {
            logger.warn("Rejected log request for unknown logger name: " + loggerName);
            return new LogResponse(loggerName, activeLoggers, 0, List.of(), now);
        }

        Path path = Paths.get(LOG_DIR, loggerName + ".log");
        if (!Files.exists(path)) {
            return new LogResponse(loggerName, activeLoggers, 0, List.of(), now);
        }

        try {
            List<String> logs = readTailWithFilter(path.toFile(), levelFilter, maxLines);
            return new LogResponse(loggerName, activeLoggers, logs.size(), logs, now);
        } catch (IOException e) {
            return new LogResponse(loggerName, activeLoggers, 0,
                    List.of("Error reading logs: " + e.getMessage()), now);
        }
    }

    private List<String> readTailWithFilter(File file, LogLevel levelFilter, int maxLines) throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            return Collections.emptyList();
        }

        Deque<String> buffer = new ArrayDeque<>(maxLines);

        String filterTag = levelFilter == LogLevel.ALL ? null : "[" + levelFilter.name() + "]";

        long fileLength = file.length();
        long estimateBytes = Math.max(1024 * 10, (long) maxLines * 250 * 2);
        long seekPos = Math.max(0, fileLength - Math.min(estimateBytes, 5 * 1024 * 1024));

        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            raf.seek(seekPos);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(Channels.newInputStream(raf.getChannel()), StandardCharsets.UTF_8))) {
                String line;
                if (seekPos > 0) {
                    reader.readLine();
                }
                while ((line = reader.readLine()) != null) {
                    if (filterTag == null || line.contains(filterTag)) {
                        buffer.addFirst(line);

                        if (buffer.size() > maxLines) {
                            buffer.removeLast();
                        }
                    }
                }
            }
        }
        return new ArrayList<>(buffer);
    }

    public List<String> getAvailableLogFiles() {
        try {
            Path logDir = Paths.get(LOG_DIR);
            if (!Files.exists(logDir)) {
                return List.of();
            }

            try (Stream<Path> paths = Files.list(logDir)) {
                return paths.filter(p -> p.toString().endsWith(".log"))
                           .map(p -> p.getFileName().toString().replace(".log", ""))
                           .sorted()
                           .toList();
            }

        } catch (IOException e) {
            logger.error("Failed to list log files", e);
            return List.of();
        }
    }
}

package dev.rezu.database;

import dev.rezu.logger.AsyncLogger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

public class DBConnectionPool {
    private static final AsyncLogger logger = AsyncLogger.getLogger(DBConnectionPool.class);

    private final String url, user, password;
    private final int poolSize;
    private final long timeout;
    private final long maxLifetime = TimeUnit.HOURS.toMillis(7);
    private final LinkedBlockingQueue<ConnectionWrapper> pool;
    private final ScheduledExecutorService healthChecker = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().name("db-pool-health-checker").factory());

    private final LongAdder totalCreated = new LongAdder();
    private final LongAdder totalTimeouts = new LongAdder();
    private final LongAdder totalValidationsFailed = new LongAdder();
    private final LongAdder totalRecycled = new LongAdder();
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public DBConnectionPool(String url, String user, String password, int poolSize, long timeout) throws SQLException {
        this.url = url;
        this.user = user;
        this.password = password;
        this.poolSize = poolSize;
        this.timeout = timeout;
        this.pool = new LinkedBlockingQueue<>(poolSize);

        for (int i = 0; i < poolSize; i++) {
            pool.add(createNewWrappedConnection());
        }

        healthChecker.scheduleAtFixedRate(this::validatePoolConnections, 30, 30, TimeUnit.SECONDS);
    }

    private ConnectionWrapper createNewWrappedConnection() throws SQLException {
        totalCreated.increment();
        return new ConnectionWrapper(DriverManager.getConnection(url, user, password));
    }

    public PooledConnection getConnection() throws SQLException {
        try {
            ConnectionWrapper wrapper = pool.poll(timeout, TimeUnit.MILLISECONDS);
            if (wrapper == null) {
                totalTimeouts.increment();
                throw new SQLException("Database pool timeout. No connection available");
            }
            activeConnections.incrementAndGet();
            return new PooledConnection(wrapper.connection, this);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new SQLException("Thread interrupted while waiting for a connection", e);
        }
    }

    private void validatePoolConnections() {
        List<ConnectionWrapper> temp = new ArrayList<>(poolSize);
        pool.drainTo(temp);

        for (ConnectionWrapper wrapper : temp) {
            boolean isBroken = false;
            try {
                if (wrapper.connection.isClosed() ||
                        (System.currentTimeMillis() - wrapper.createdAt > maxLifetime) ||
                        !wrapper.connection.isValid(1)) {
                    isBroken = true;
                }
            } catch (SQLException e) {
                logger.error("Exception with connection validation: ", e);
                isBroken = true;
            }

            if (isBroken) {
                totalValidationsFailed.increment();
                try { wrapper.connection.close(); } catch (SQLException _) {}
                try {
                    wrapper = createNewWrappedConnection();
                    logger.warn("Replaced invalid/stale connection during health check");
                } catch (SQLException e) {
                    logger.error("Failed to replace broken connection", e);
                    continue;
                }
            }
            pool.offer(wrapper);
        }
        logger.debug("Background health check completed. Pool size: " + pool.size());
    }

    public void releaseConnection(Connection conn) {
        if (conn == null) return;
        activeConnections.decrementAndGet();

        try {
            if (conn.isClosed()) {
                return;
            }

            if (!conn.getAutoCommit()) {
                conn.rollback();
                conn.setAutoCommit(true);
            }
            conn.clearWarnings();

            if (pool.offer(new ConnectionWrapper(conn))) {
                totalRecycled.increment();
            } else {
                conn.close();
            }
        } catch (SQLException e) {
            try { conn.close(); } catch (SQLException _) {}
            logger.error("Discarding broken connection on release", e);
        }
    }

    public static boolean isConnectionError(SQLException e) {
        String state = e.getSQLState();
        return state != null && state.startsWith("08");
    }

    public void shutdown() {
        healthChecker.shutdown();
        ConnectionWrapper wrapper;
        while ((wrapper = pool.poll()) != null) {
            try {
                wrapper.connection.close();
            } catch (SQLException _) {}
        }
    }

    public DatabasePoolMetrics getMetrics() {
        return new DatabasePoolMetrics(
                poolSize,
                Math.max(0, activeConnections.get()),
                pool.size(),
                totalCreated.sum(),
                totalTimeouts.sum(),
                totalValidationsFailed.sum(),
                totalRecycled.sum(),
                isHealthy(),
                poolSize == 0 ? 0.0 : (activeConnections.get() * 100.0) / poolSize
        );
    }

    public boolean isHealthy() {
        return activeConnections.get() < poolSize;
    }

    private static class ConnectionWrapper {
        final Connection connection;
        final long createdAt;
        ConnectionWrapper(Connection connection) {
            this.connection = connection;
            this.createdAt = System.currentTimeMillis();
        }
    }
}
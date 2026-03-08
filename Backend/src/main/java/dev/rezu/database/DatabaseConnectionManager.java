package dev.rezu.database;

import dev.rezu.logger.AsyncLogger;

import java.sql.SQLException;

public class DatabaseConnectionManager {
    private final DBConnectionPool connectionPool;
    private static final AsyncLogger logger = AsyncLogger.getLogger(DatabaseConnectionManager.class);

    public DatabaseConnectionManager(String username, String password) {
        try {
            String url = System.getenv("DB_URL");
            this.connectionPool = new DBConnectionPool(url, username, password, 15, 5000);
            Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
        } catch (SQLException e) {
            throw new RuntimeException("Error while trying to initialize connection pool", e);
        }
    }

    public PooledConnection getConnection() throws SQLException {
        return connectionPool.getConnection();
    }

    public void shutdown() {
        logger.info("Shutting down Database Connection Pool...");
        connectionPool.shutdown();
    }

    public DatabasePoolMetrics getPoolMetrics() {
        return connectionPool.getMetrics();
    }

    public boolean isPoolHealthy() {
        return connectionPool.isHealthy();
    }
}
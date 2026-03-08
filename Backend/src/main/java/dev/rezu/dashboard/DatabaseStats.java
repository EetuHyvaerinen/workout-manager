package dev.rezu.dashboard;

import dev.rezu.database.DatabasePoolMetrics;

public record DatabaseStats(
        int poolSize,
        int activeConnections,
        int idleConnections,
        long totalConnectionsCreated,
        long connectionTimeouts,
        long validationsFailed,
        long totalRecycled,
        double loadPercentage,
        boolean healthy
) {
    public static DatabaseStats fromMetrics(DatabasePoolMetrics metrics, boolean validatedHealthy) {
        return new DatabaseStats(
                metrics.poolSize(),
                metrics.activeConnections(),
                metrics.idleConnections(),
                metrics.totalCreated(),
                metrics.totalTimeouts(),
                metrics.totalValidationsFailed(),
                metrics.totalRecycled(),
                metrics.loadPercentage(),
                validatedHealthy && metrics.hasCapacity()
        );
    }
}
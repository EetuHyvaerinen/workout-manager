package dev.rezu.database;

public record DatabasePoolMetrics(
        int poolSize,
        int activeConnections,
        int idleConnections,
        long totalCreated,
        long totalTimeouts,
        long totalValidationsFailed,
        long totalRecycled,
        boolean isHealthy,
        double loadPercentage
) {}
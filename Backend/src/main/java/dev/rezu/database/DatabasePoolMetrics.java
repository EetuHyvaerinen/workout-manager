package dev.rezu.database;

public record DatabasePoolMetrics(
        int poolSize,
        int activeConnections,
        int idleConnections,
        long totalCreated,
        long totalTimeouts,
        long totalValidationsFailed,
        long totalRecycled
) {

    public double loadPercentage() {
        return poolSize == 0 ? 0.0 : (activeConnections * 100.0) / poolSize;
    }

    public boolean hasCapacity() {
        return idleConnections > 0;
    }
}
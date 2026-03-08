package dev.rezu.dashboard;

public record ServerStats(
        long uptimeSeconds,
        long totalMemoryMB,
        long usedMemoryMB,
        long freeMemoryMB,
        int availableProcessors,
        long threadCount,
        int loggerQueueSize,
        int loggerCapacity,
        long droppedCount) {}

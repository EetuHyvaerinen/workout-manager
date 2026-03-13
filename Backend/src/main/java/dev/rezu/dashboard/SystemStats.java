package dev.rezu.dashboard;

import dev.rezu.database.DatabasePoolMetrics;

import java.time.Instant;

public record SystemStats(
        long totalUsers,
        long totalWorkouts,
        long totalExercises,
        long activeUsers24h,
        long workoutsToday,
        DatabasePoolMetrics databaseStats,
        ServerStats serverStats,
        Instant collectedAt
) {
}

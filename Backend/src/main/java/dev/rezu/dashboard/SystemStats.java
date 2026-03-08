package dev.rezu.dashboard;

import java.time.Instant;

public record SystemStats(
        long totalUsers,
        long totalWorkouts,
        long totalExercises,
        long activeUsers24h,
        long workoutsToday,
        DatabaseStats databaseStats,
        ServerStats serverStats,
        Instant collectedAt
) {
}

package dev.rezu.dashboard;

import java.time.Instant;
import java.util.List;

public record RecentActivity(
        List<UserActivity> recentLogins,
        List<WorkoutActivity> recentWorkouts,
        List<ErrorActivity> recentErrors
) {
    public record UserActivity(int userId, String email, String action) {}
    public record WorkoutActivity(String workoutId, int userId, String workoutName, int exerciseCount, Instant createdAt) {}
    public record ErrorActivity(String errorType, String message, String endpoint) {}
}
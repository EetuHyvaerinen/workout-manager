package dev.rezu.dashboard;

import dev.rezu.logger.AsyncLogger;
import dev.rezu.database.PooledConnection;

import java.sql.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class DashboardDAO {

    private static final AsyncLogger logger = AsyncLogger.getLogger(DashboardDAO.class);

    public record DbCounts(
            long totalUsers,
            long totalWorkouts,
            long totalExercises,
            long activeUsers24h,
            long workoutsToday
    ) {}

    public DbCounts getAggregatedDbCounts(PooledConnection conn) throws SQLException {
        String sql = """
        SELECT
            (SELECT COUNT(*) FROM users) as totalUsers,
            (SELECT COUNT(*) FROM workouts) as totalWorkouts,
            (SELECT COUNT(*) FROM exercises) as totalExercises,
            (SELECT COUNT(DISTINCT user_id) FROM user_activity WHERE activity_timestamp >= NOW() - INTERVAL 1 DAY) as activeUsers24h,
            (SELECT COUNT(*) FROM workouts WHERE created_at >= ?) as workoutsToday
        """;

        Instant startOfDay = Instant.now().truncatedTo(ChronoUnit.DAYS);

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setTimestamp(1, Timestamp.from(startOfDay));

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new DbCounts(
                            rs.getLong("totalUsers"),
                            rs.getLong("totalWorkouts"),
                            rs.getLong("totalExercises"),
                            rs.getLong("activeUsers24h"),
                            rs.getLong("workoutsToday")
                    );
                }
            }
        }
        return new DbCounts(0, 0, 0, 0, 0);
    }
}

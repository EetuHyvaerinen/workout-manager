package dev.rezu.dashboard;

import dev.rezu.logger.AsyncLogger;
import dev.rezu.database.PooledConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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

        Instant startOfDay = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.DAYS);

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

    public List<RecentActivity.UserActivity> getRecentLogins(PooledConnection conn, int limit) throws SQLException {
        String sql = """
            SELECT ua.user_id, u.email, ua.action, ua.activity_timestamp
            FROM user_activity ua
            JOIN users u ON ua.user_id = u.id
            ORDER BY ua.activity_timestamp DESC
            LIMIT ?
            """;

        List<RecentActivity.UserActivity> activities = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(new RecentActivity.UserActivity(
                            rs.getInt("user_id"),
                            rs.getString("email"),
                            rs.getString("action"),
                            rs.getTimestamp("activity_timestamp").toInstant()
                    ));
                }
            }
        }
        return activities;
    }

    public List<RecentActivity.WorkoutActivity> getRecentWorkouts(PooledConnection conn, int limit)
            throws SQLException {
        String sql = """
            SELECT
                w.id,
                w.user_id,
                w.name,
                w.created_at,
                COUNT(e.id) as exercise_count
            FROM workouts w
            LEFT JOIN exercises e ON w.id = e.workout_id
            GROUP BY w.id, w.user_id, w.name, w.created_at
            ORDER BY w.created_at DESC
            LIMIT ?
            """;
        
        List<RecentActivity.WorkoutActivity> activities = new ArrayList<>();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    activities.add(new RecentActivity.WorkoutActivity(
                        rs.getString("id"),
                        rs.getInt("user_id"),
                        rs.getString("name"),
                        rs.getInt("exercise_count"),
                        rs.getTimestamp("created_at").toInstant()
                    ));
                }
            }
        }
        return activities;
    }

    public List<TimeSeriesData> getUserGrowth(PooledConnection conn, int days) 
            throws SQLException {
        String sql = """
            SELECT
                DATE(created_at) as date,
                COUNT(*) as count
            FROM users
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY)
            GROUP BY DATE(created_at)
            ORDER BY date
            """;

        return getTimeSeriesData(conn, days, sql);
    }

    public List<TimeSeriesData> getWorkoutTrends(PooledConnection conn, int days) 
            throws SQLException {
        String sql = """
            SELECT
                DATE(created_at) as date,
                COUNT(*) as count
            FROM workouts
            WHERE created_at >= DATE_SUB(NOW(), INTERVAL ? DAY)
            GROUP BY DATE(created_at)
            ORDER BY date
            """;

        return getTimeSeriesData(conn, days, sql);
    }

    private List<TimeSeriesData> getTimeSeriesData(PooledConnection conn, int days, String sql) throws SQLException {
        List<TimeSeriesData> data = new ArrayList<>();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    data.add(new TimeSeriesData(
                        rs.getDate("date").toLocalDate().toString(),
                        rs.getLong("count")
                    ));
                }
            }
        }
        return data;
    }

    public void logUserActivity(PooledConnection conn, int userId, String action) throws SQLException {
        String sql = "INSERT INTO user_activity (user_id, action, activity_timestamp) VALUES (?, ?, NOW())";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, action);
            pstmt.executeUpdate();
        }
    }
}

package dev.rezu.workout;

import dev.rezu.database.PooledConnection;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class WorkoutDAO {

    public String createWorkout(PooledConnection conn, int userId, String name) throws SQLException {
        String sql = "INSERT INTO workouts (id, user_id, name) VALUES (?, ?, ?)";
        String uuid = UUID.randomUUID().toString();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setInt(2, userId);
            pstmt.setString(3, name);
            pstmt.executeUpdate();
            return uuid;
        }
    }

    public String createWorkout(PooledConnection conn, int userId, String name, Instant time) throws SQLException {
        String sql = "INSERT INTO workouts (id, user_id, name, created_at) VALUES (?, ?, ?, ?)";
        String uuid = UUID.randomUUID().toString();

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            pstmt.setInt(2, userId);
            pstmt.setString(3, name);
            pstmt.setObject(4, time);
            pstmt.executeUpdate();
            return uuid;
        }
    }

    public List<Workout> getWorkouts(PooledConnection conn, Instant start, Instant end, int userId) throws SQLException {
        String sql = "SELECT * FROM workouts WHERE created_at >= ? AND created_at < ? AND user_id = ?";
        List<Workout> workouts = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setObject(1, start);
            pstmt.setObject(2, end);
            pstmt.setInt(3, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    workouts.add(new Workout(
                            rs.getString("id"),
                            rs.getInt("user_id"),
                            rs.getString("name"),
                            rs.getTimestamp("created_at").toInstant()
                    ));
                }
            }
            return workouts;
        }
    }

    public Workout getWorkout(PooledConnection conn, String id, int userId) throws SQLException {
        String sql = "SELECT * FROM workouts WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Workout(
                            id,
                            userId,
                            rs.getString("name"),
                            rs.getTimestamp("created_at").toInstant()
                    );
                }
                return null;
            }
        }
    }

    public List<Workout> getWorkouts(PooledConnection conn, int userId) throws SQLException {
        List<Workout> workouts = new ArrayList<>();
        String sql = "SELECT * FROM workouts WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    workouts.add(new Workout(
                            rs.getString("id"),
                            userId,
                            rs.getString("name"),
                            rs.getTimestamp("created_at").toInstant()
                    ));
                }
            }
        }
        return workouts;
    }

    public boolean updateWorkout(PooledConnection conn, Workout workout) throws SQLException {
        String sql = "UPDATE workouts SET updated_at = NOW() WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, workout.id());
            return pstmt.executeUpdate() > 0;
        }
    }

    public boolean deleteWorkout(PooledConnection conn, String id, int userId) throws SQLException {
        String sql = "DELETE FROM workouts WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setInt(2, userId);
            return pstmt.executeUpdate() > 0;
        }
    }
}
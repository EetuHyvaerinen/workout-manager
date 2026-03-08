package dev.rezu.user;
import dev.rezu.logger.AsyncLogger;
import dev.rezu.database.PooledConnection;

import java.sql.*;


public class UserDAO {
    private static final AsyncLogger logger = AsyncLogger.getLogger(UserDAO.class);


    public int createUser(PooledConnection conn, User user) throws SQLException {
        String sql = "INSERT INTO users " +
                "(email, salt, password_hash, username, created_at) " +
                "VALUES (?, ?, ?, ?, ?)";
        int userId;

        try(PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, user.email());
            pstmt.setString(2, user.salt());
            pstmt.setString(3, user.passwordHash());
            pstmt.setString(4, user.name());
            pstmt.setObject(5, user.createdAt());
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    userId = generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating user failed, no ID obtained.");
                }
            }

            return userId;
        }
    }

    public User getUserByEmail(PooledConnection conn, String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("salt"),
                            rs.getString("password_hash"),
                            rs.getBoolean("is_admin"),
                            rs.getTimestamp("created_at").toInstant()
                    );
                }
            }
            return null;
        }
    }

    public User getUserById(PooledConnection conn, int userId) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("email"),
                            rs.getString("username"),
                            rs.getString("salt"),
                            rs.getString("password_hash"),
                            rs.getBoolean("is_admin"),
                            rs.getTimestamp("created_at").toInstant()
                    );
                }
                return null;
            }
        }
    }

    public boolean deleteUser(String uuid, PooledConnection conn) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, uuid);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected == 1;

        }
    }

    public void logUserActivity(PooledConnection conn, int userId, String action) throws SQLException {
        if (userId <= 0) return;
        String sql = "INSERT INTO user_activity (user_id, action, activity_timestamp) VALUES (?, ?, NOW())";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, action);
            pstmt.executeUpdate();
        }
    }
}

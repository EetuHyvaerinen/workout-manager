package dev.rezu.user;

import dev.rezu.logger.AsyncLogger;
import dev.rezu.BaseHandler;
import dev.rezu.database.DatabaseConnectionManager;
import dev.rezu.database.PooledConnection;

import java.sql.SQLException;

public class UserService {
    private static final AsyncLogger logger = AsyncLogger.getLogger(UserService.class);

    private final DatabaseConnectionManager connectionManager;
    private final UserDAO userDAO;

    public UserService(DatabaseConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.userDAO = new UserDAO();
    }

    public PooledConnection getConnection() throws SQLException {
        return connectionManager.getConnection();
    }

    public int createUser(String email, String salt, String hashedPassword) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);

            User existingUser = userDAO.getUserByEmail(conn, email);
            if (existingUser != null) {
                return 0;
            }

            User newUser = new User(
                    email,
                    "",
                    salt,
                    hashedPassword
            );
            int userId = userDAO.createUser(conn, newUser);
            conn.commit();
            return userId;
        } catch (SQLException e) {
            logger.error("Failed to create user: " + email, e);
            return -1;
        }
    }

    public User getUserById(int userId) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            return userDAO.getUserById(conn, userId);
        } catch (SQLException e) {
            logger.error("Error fetching user by ID: " + userId, e);
            return null;
        }
    }

    public User authenticateUser(String email, String password) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            User user = userDAO.getUserByEmail(conn, email);

            if (user == null) return null;

            if (BaseHandler.getAuthenticator().verifyPassword(password, user.salt(), user.passwordHash())) {
                logger.info("Authenticated email: " + email + " with userId: " + user.id());
                return user;
            }
        } catch (SQLException e) {
            logger.error("Authentication error for user: " + email, e);
        }
        return null;
    }

    public boolean deleteUser(String userId) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            return userDAO.deleteUser(userId, conn);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void logUserActivity(int userId, String action) {
        try (PooledConnection conn = connectionManager.getConnection()) {
            userDAO.logUserActivity(conn, userId, action);
        } catch (SQLException e) {
            logger.error("Failed to log activity '" + action + "' for user " + userId, e);
        }
    }
}

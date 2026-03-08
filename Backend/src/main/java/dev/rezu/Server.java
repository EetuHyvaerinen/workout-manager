package dev.rezu;

import com.sun.net.httpserver.HttpServer;
import dev.rezu.dashboard.DashboardHandler;
import dev.rezu.dashboard.DashboardService;
import dev.rezu.database.DatabaseConnectionManager;
import dev.rezu.logger.AsyncLogger;
import dev.rezu.user.UserHandler;
import dev.rezu.user.UserService;
import dev.rezu.workout.WorkoutHandler;
import dev.rezu.workout.WorkoutService;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
    private static final AsyncLogger logger = AsyncLogger.getLogger(Server.class);
    private static final int SERVER_PORT = 8444;

    void main() {
        try {
            RateLimiter rateLimiter = BaseHandler.getRateLimiter();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Shutting down RateLimitManager");
                rateLimiter.shutdown();
            }));

            HttpServer httpServer = HttpServer.create(new InetSocketAddress(SERVER_PORT), 0);
            httpServer.setExecutor(Executors.newVirtualThreadPerTaskExecutor());

            logger.info("Initializing database connection manager");
            var dbManager = new DatabaseConnectionManager(System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));

            var workoutService = new WorkoutService(dbManager);
            var workoutHandler = new WorkoutHandler(workoutService);

            var userService = new UserService(dbManager);
            var userHandler = new UserHandler(userService);

            var dashboardService = new DashboardService(dbManager);
            var dashboardHandler = new DashboardHandler(dashboardService);

            //Register API endpoints
            logger.info("Registering API endpoints");

            httpServer.createContext("/api/workout", workoutHandler);

            httpServer.createContext("/api/user/me", userHandler);
            httpServer.createContext("/api/login", userHandler);
            httpServer.createContext("/api/register", userHandler);
            httpServer.createContext("/api/logout", userHandler);

            //Dashboard endpoints
            httpServer.createContext("/api/dashboard", dashboardHandler);
            httpServer.createContext("/api/dashboard/stats", dashboardHandler);
            httpServer.createContext("/api/dashboard/logs", dashboardHandler);
            httpServer.createContext("/api/dashboard/activity", dashboardHandler);
            httpServer.createContext("/api/dashboard/trends", dashboardHandler);
            httpServer.createContext("/api/dashboard/health", dashboardHandler);

            httpServer.start();
            logger.info("HTTP server started successfully on port " + SERVER_PORT);
        } catch (Exception e) {
            logger.error("HTTP server startup failed", e);
        }
    }
}

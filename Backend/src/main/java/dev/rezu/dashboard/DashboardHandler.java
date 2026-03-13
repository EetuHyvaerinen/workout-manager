package dev.rezu.dashboard;

import com.sun.net.httpserver.HttpExchange;
import dev.rezu.*;
import dev.rezu.logger.AsyncLogger;
import dev.rezu.response.ResponseMessage;
import dev.rezu.response.ResponseMessageType;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class DashboardHandler extends BaseHandler {
    
    private static final AsyncLogger logger = AsyncLogger.getLogger(DashboardHandler.class);
    
    private final DashboardService dashboardService;

    public DashboardHandler(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }
    
    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getQuery();

        if (!auth().isAdmin()) {
            logger.warn("Unauthorized dashboard access attempt by userId: " + auth().userId());
            ResponseMessage.send(exchange, 403, ResponseMessageType.ERROR,
                    "Access denied. Admin privileges required.");
            return;
        }
        
        logger.info("Admin user " + auth().userId() + " accessing dashboard: " + path);
        
        try {
            if (path.endsWith("/dashboard/stats")) {
                handleStats(exchange);
            } else if (path.endsWith("/dashboard/logs")) {
                handleLogs(exchange, query);
            } else {
                handleOverview(exchange);
            }
        } catch (Exception e) {
            logger.error("Dashboard request failed", e);
            ResponseMessage.send(exchange, 500, ResponseMessageType.ERROR, 
                "Internal server error: " + e.getMessage());
        }
    }

    private void handleOverview(HttpExchange exchange) throws IOException {
        SystemStats stats = dashboardService.getSystemStats();
        List<String> availableLogs = dashboardService.getAvailableLogFiles();

        DashboardOverview overview = new DashboardOverview(stats, availableLogs, "Dashboard overview");

        ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, overview);
    }

    private void handleStats(HttpExchange exchange) throws IOException {
        SystemStats stats = dashboardService.getSystemStats();
        ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, stats);
    }

    private void handleLogs(HttpExchange exchange, String query) throws IOException {
        String requestedName = "Server";
        String levelFilter = "ALL";
        int maxLines = 100;

        if (query != null) {
            for (String param : query.split("&")) {
                String[] kv = param.split("=");
                if (kv.length == 2) {
                    String value = URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
                    switch (kv[0]) {
                        case "logger" -> requestedName = value;
                        case "level"  -> levelFilter = value;
                        case "lines"  -> {
                            try { maxLines = Math.min(Integer.parseInt(value), 1000); }
                            catch (NumberFormatException ignored) {}
                        }
                    }
                }
            }
        }

        LogResponse logs = dashboardService.getLogs(requestedName, levelFilter, maxLines);
        ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, logs);
    }
}

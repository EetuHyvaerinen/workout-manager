package dev.rezu;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import dev.rezu.auth.AuthExtractor;
import dev.rezu.auth.AuthResult;
import dev.rezu.auth.Authenticator;
import dev.rezu.logger.AsyncLogger;
import dev.rezu.response.ResponseMessage;
import dev.rezu.response.ResponseMessageType;

import java.io.IOException;

public abstract class BaseHandler implements HttpHandler {

    private static final AsyncLogger logger = AsyncLogger.getLogger(BaseHandler.class);
    private static final Authenticator authenticator = new Authenticator();
    private static final RateLimiter rateLimiter = new RateLimiter();
    private static final AuthExtractor authExtractor = new AuthExtractor(authenticator);

    public static final ScopedValue<AuthResult> AUTH_CONTEXT = ScopedValue.newInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long startTime = System.currentTimeMillis();

        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "http://localhost:8080");
        exchange.getResponseHeaders().set("Access-Control-Allow-Credentials", "true");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS, DELETE");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }

        String clientIp = exchange.getRequestHeaders().getFirst("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
        } else {
            clientIp = clientIp.split(",")[0].trim();
        }

        if (!rateLimiter.allowRequest(clientIp)) {
            exchange.getResponseHeaders().set("Retry-After", "60");
            ResponseMessage.send(exchange, 429, ResponseMessageType.ERROR, "Too many requests");
            return;
        }
        AuthResult authResult = authExtractor.authenticate(exchange);
        int userId = authResult != null ? authResult.userId() : -1;
        boolean isAdmin = authResult != null && authResult.isAdmin();

        String path = exchange.getRequestURI().getPath();
            //check if authentication is required
            boolean isPublicEndpoint = path.equals("/api/register")
                    || path.equals("/api/login")
                    || path.equals("/api/logout");

            if (userId == -1 && !isPublicEndpoint) {
                ResponseMessage.send(exchange, 401, ResponseMessageType.ERROR, "Unauthorized");
                return;
            }
        try {
            ScopedValue.where(AUTH_CONTEXT, authResult).call(() -> {
                String method = exchange.getRequestMethod().toUpperCase();
                switch (method) {
                    case "GET" -> handleGet(exchange);
                    case "POST" -> handlePost(exchange);
                    case "DELETE" -> handleDelete(exchange);
                    default -> exchange.sendResponseHeaders(405, -1);
                }                return null;
            });
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request at " + exchange.getRequestURI() + ": " + e.getMessage());
            ResponseMessage.send(exchange, 400, ResponseMessageType.ERROR, e.getMessage());
        } catch (IOException e) {
            logger.error("Handler error at " + exchange.getRequestURI(), e);
            exchange.sendResponseHeaders(500, -1);
        } catch (RuntimeException e) {
            logger.error("Unexpected error at " + exchange.getRequestURI(), e);
            try {
                ResponseMessage.send(exchange, 500, ResponseMessageType.ERROR, "Internal server error");
            } catch (IOException _) {
            }
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            logger.info(exchange.getRequestMethod().toUpperCase() + " " +
                    exchange.getRequestURI().getPath() +
                    (exchange.getRequestURI().getQuery() != null ? "?" + exchange.getRequestURI().getQuery() : "") +
                    " | userId=" + userId +
                    " | client=" + clientIp +
                    " | duration=" + duration + " ms");
            exchange.close();
        }
    }

    public static Authenticator getAuthenticator() {
        return authenticator;
    }

    protected static AuthResult auth() {
        return AUTH_CONTEXT.get();
    }

    protected void handleGet(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
    }

    protected void handlePost(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
    }

    protected void handleDelete(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
    }

    public static RateLimiter getRateLimiter() {
        return rateLimiter;
    }
}
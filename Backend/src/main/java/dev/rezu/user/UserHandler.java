package dev.rezu.user;

import com.sun.net.httpserver.HttpExchange;
import dev.rezu.*;
import dev.rezu.auth.Authenticator;
import dev.rezu.json.JsonReader;
import dev.rezu.json.JsonNode;
import dev.rezu.logger.AsyncLogger;
import dev.rezu.response.ResponseMessage;
import dev.rezu.response.ResponseMessageType;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class UserHandler extends BaseHandler {
    private static final AsyncLogger logger = AsyncLogger.getLogger(UserHandler.class);

    private final UserService userService;

    public UserHandler(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void handleGet(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.endsWith("/me")) {
            User user = userService.getUserById(auth().userId());

            if (user == null) {
                ResponseMessage.send(exchange, 404, "User not found");
                return;
            }
            ResponseMessage.send(exchange, 200, ResponseMessageType.DATA, user);
        } else {
            ResponseMessage.send(exchange, 404, ResponseMessageType.ERROR, "Endpoint not found");
        }
    }


    @Override
    protected void handlePost(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        logger.info("Received POST request for path: " + path);
        if (path.endsWith("/register")) {
            handleRegistration(exchange);
        }
        else if (path.endsWith("/login")) {
            handleLogin(exchange);
        }
        else if (path.endsWith("/logout")) {
            handleLogout(exchange);
        }  else  {
            logger.warn("Unknown POST endpoint: " + path);
            ResponseMessage.send(exchange, 404, ResponseMessageType.ERROR, "Endpoint not found");
        }
    }

    @Override
    protected void handleDelete(HttpExchange exchange) {
    }

    private void handleRegistration(HttpExchange exchange) throws IOException {
        JsonNode jsonNode;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            jsonNode = JsonReader.parse(reader);
        }
        String email = jsonNode.str("email");
        String password = jsonNode.str("password");

        if (email.isEmpty() || password.isEmpty()) {
            logger.warn("Registration failed: Missing email or password in request body");
            ResponseMessage.send(exchange, 400, ResponseMessageType.ERROR, "Email and password required");
            return;
        }
        logger.info("Attempting to register user: " + email);
        String salt = Authenticator.getNewSalt();
        String hashed = getAuthenticator().hashPassword(password, salt);
        int userId = userService.createUser(email, salt, hashed);

        if (userId == 0) {
            logger.warn("Registration conflict: User already exists with email: " + email);
            ResponseMessage.send(exchange, 409, ResponseMessageType.ERROR, "User already exists");
        } else if (userId < 0) {
            logger.error("Registration failed due to internal error for email: " + email);
            ResponseMessage.send(exchange, 500, ResponseMessageType.ERROR, "Internal server error");
        } else {
            userService.logUserActivity(userId, "registered");
            logger.info("User registered successfully. Assigned ID: " + userId);
            ResponseMessage.send(exchange, 201, ResponseMessageType.MESSAGE, "Registration successful");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        JsonNode jsonNode;
        try (InputStreamReader reader = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8)) {
            jsonNode = JsonReader.parse(reader);
        }
        String email = jsonNode.str("email");
        String password = jsonNode.str("password");

        if (email.isEmpty() || password.isEmpty()) {
            logger.warn("Login failed: Missing email or password in request body");
            ResponseMessage.send(exchange, 400, ResponseMessageType.ERROR, "Email and password required");
            return;
        }

        User user = userService.authenticateUser(email, password);

        if (user == null) {
            logger.warn("Failed login attempt");
            ResponseMessage.send(exchange, 401, ResponseMessageType.ERROR, "Invalid credentials");
            return;
        }

        String accessToken = getAuthenticator().createJwtToken(user.id(), user.isAdmin(), 3600);
        String cookieValue = String.format(
                "accessToken=%s; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=3600",
                accessToken
        );

        exchange.getResponseHeaders().add("Set-Cookie", cookieValue);
        userService.logUserActivity(user.id(), "login");
        logger.info("User login successful. ID: " + user.id());
        ResponseMessage.send(exchange, 200, ResponseMessageType.MESSAGE, "Login successful");
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        int userId = auth().userId();
        logger.info("Processing logout");
        String cookieValue = "accessToken=; Path=/; HttpOnly; Secure; SameSite=Strict; Max-Age=0";
        exchange.getResponseHeaders().add("Set-Cookie", cookieValue);
        userService.logUserActivity(userId, "logout");
        logger.info("User logged out successfully.");
        ResponseMessage.send(exchange,200,"Logged out");
    }
}
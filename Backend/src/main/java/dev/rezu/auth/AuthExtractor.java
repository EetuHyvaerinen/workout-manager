package dev.rezu.auth;

import com.sun.net.httpserver.HttpExchange;
import dev.rezu.logger.AsyncLogger;

public class AuthExtractor {
    private final Authenticator authenticator;
    private final AsyncLogger logger = AsyncLogger.getLogger(AuthExtractor.class);

    public AuthExtractor(Authenticator authenticator) {
        this.authenticator = authenticator;
    }

    public AuthResult authenticate(HttpExchange exchange) {
        String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
        AuthResult anonymous = new AuthResult(-1, false);
        if (cookieHeader == null || cookieHeader.isEmpty()) return anonymous;

        int tokenIndex = cookieHeader.indexOf("accessToken=");
        if (tokenIndex == -1) return anonymous;

        int tokenStart = tokenIndex + "accessToken=".length();
        int tokenEnd = cookieHeader.indexOf(';', tokenStart);
        String token = (tokenEnd == -1)
                ? cookieHeader.substring(tokenStart).trim()
                : cookieHeader.substring(tokenStart, tokenEnd).trim();

        if (token.isEmpty()) return anonymous;

        AuthResult result = authenticator.verifyJwtFull(token);

        if (result != null && result.userId() > 0) {
            logger.debug("User authenticated successfully: userId: " + result.userId());
            return result;
        }

        logger.debug("Authentication failed: invalid or expired token");
        return anonymous;
    }
}
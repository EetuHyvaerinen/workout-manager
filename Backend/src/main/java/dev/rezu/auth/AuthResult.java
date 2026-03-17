package dev.rezu.auth;

public record AuthResult(int userId, boolean isAdmin) {
    public boolean isAuthenticated() {
        return userId > 0;
    }
}

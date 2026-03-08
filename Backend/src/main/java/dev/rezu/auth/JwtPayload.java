package dev.rezu.auth;

public record JwtPayload(String sub, boolean admin, long iat, long exp) {}
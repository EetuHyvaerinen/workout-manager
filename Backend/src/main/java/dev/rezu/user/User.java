package dev.rezu.user;

import java.time.Instant;

public record User(
        int id,
        String email,
        String name,
        String salt,
        String passwordHash,
        boolean isAdmin,
        Instant createdAt
) {

    public User(String email, String name, String salt, String passwordHash) {
        this(0, email, name, salt, passwordHash, false, Instant.now());
    }

    public User(int id, String email, String name, boolean isAdmin) {
        this(id, email, name, null, null, isAdmin, null);
    }
}
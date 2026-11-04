package com.datatrust.domain.users;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.datatrust.domain.enums.UserRole;

public class User {

    private final UUID id;
    private final String username;
    private final String passwordHash;
    private final UserRole role;
    private final String displayName;
    private final String email;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    private User(UUID id, String username, String passwordHash, UserRole role,
                 String displayName, String email, boolean active,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = displayName;
        this.email = email;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User create(String username, String passwordHash, UserRole role,
                              String displayName, String email) {
        UUID id = UUID.randomUUID();
        return create(id, username, passwordHash, role, displayName, email);
    }

    public static User create(UUID id, String username, String passwordHash, UserRole role,
                              String displayName, String email) {
        String cleanUsername = requireText(username, "username");
        String cleanPasswordHash = requirePasswordHash(passwordHash);
        Objects.requireNonNull(role, "role");
        String cleanDisplayName = requireText(displayName, "displayName");
        String cleanEmail = requireText(email, "email");
        Instant now = Instant.now();
        return new User(Objects.requireNonNull(id, "id"), cleanUsername, cleanPasswordHash,
                role, cleanDisplayName, cleanEmail, true, now, now);
    }

    public static User restore(UUID id, String username, String passwordHash, UserRole role,
                               String displayName, String email, boolean active,
                               Instant createdAt, Instant updatedAt) {
        return new User(id, username, passwordHash, role, displayName, email, active,
                createdAt, updatedAt);
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacío");
        }
        return value.strip();
    }

    private static String requirePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash no puede estar vacío");
        }
        return passwordHash;
    }

    public UUID id() { return id; }
    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
    public UserRole role() { return role; }
    public String displayName() { return displayName; }
    public String email() { return email; }
    public boolean active() { return active; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}

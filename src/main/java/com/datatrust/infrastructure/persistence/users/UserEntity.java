package com.datatrust.infrastructure.persistence.users;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.datatrust.domain.enums.UserRole;
import com.datatrust.domain.users.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "password_hash", nullable = false, columnDefinition = "text")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 32)
    private UserRole role;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "email", nullable = false, length = 320)
    private String email;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserEntity() {
    }

    public UserEntity(User user) {
        id = user.id();
        username = user.username();
        passwordHash = user.passwordHash();
        role = user.role();
        displayName = user.displayName();
        email = user.email();
        active = user.active();
        createdAt = user.createdAt();
        updatedAt = user.updatedAt();
    }

    public User toDomain() {
        return User.restore(id, username, passwordHash, role, displayName, email,
                active, createdAt, updatedAt);
    }
}

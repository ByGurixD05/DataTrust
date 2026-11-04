package com.datatrust.domain.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.datatrust.domain.enums.UserRole;

class UserTest {

    @Test
    void createsActiveUserWithGeneratedIdAndTimestamps() {
        User user = validUser();

        assertThat(user.id()).isNotNull();
        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.passwordHash()).isEqualTo("$hash ");
        assertThat(user.role()).isEqualTo(UserRole.DATA_STEWARD);
        assertThat(user.displayName()).isEqualTo("Alice Smith");
        assertThat(user.email()).isEqualTo("alice@example.com");
        assertThat(user.active()).isTrue();
        assertThat(user.createdAt()).isNotNull();
        assertThat(user.updatedAt()).isEqualTo(user.createdAt());
    }

    @Test
    void acceptsExplicitId() {
        UUID id = UUID.randomUUID();

        User user = User.create(id, "alice", "hash", UserRole.ADMIN, "Alice",
                "alice@example.com");

        assertThat(user.id()).isEqualTo(id);
        assertThat(user.role()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void stripsSurroundingSpacesFromTextFieldsAndPreservesPasswordHash() {
        User user = User.create(" alice ", " hash ", UserRole.DATA_STEWARD,
                " Alice Smith ", " alice@example.com ");

        assertThat(user.username()).isEqualTo("alice");
        assertThat(user.passwordHash()).isEqualTo(" hash ");
        assertThat(user.displayName()).isEqualTo("Alice Smith");
        assertThat(user.email()).isEqualTo("alice@example.com");
    }

    @Test
    void rejectsBlankRequiredText() {
        assertThatThrownBy(() -> User.create(" ", "hash", UserRole.ADMIN,
                "Alice", "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.create("alice", " ", UserRole.ADMIN,
                "Alice", "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.create("alice", "hash", UserRole.ADMIN,
                " ", "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> User.create("alice", "hash", UserRole.ADMIN,
                "Alice", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void restorePreservesStoredStateWithoutValidatingIt() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2025-02-01T00:00:00Z");

        User user = User.restore(id, "", "", null, "", "", false, createdAt, updatedAt);

        assertThat(user.id()).isEqualTo(id);
        assertThat(user.active()).isFalse();
        assertThat(user.createdAt()).isEqualTo(createdAt);
        assertThat(user.updatedAt()).isEqualTo(updatedAt);
    }

    private static User validUser() {
        return User.create(" alice ", "$hash ", UserRole.DATA_STEWARD,
                " Alice Smith ", " alice@example.com ");
    }
}

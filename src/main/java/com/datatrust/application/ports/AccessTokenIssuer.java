package com.datatrust.application.ports;

import java.time.Instant;
import java.util.UUID;

import com.datatrust.domain.users.User;

public interface AccessTokenIssuer {
    IssuedAccessToken issue(User user);

    record IssuedAccessToken(String value, Instant expiresAt, UUID userId) {
    }
}

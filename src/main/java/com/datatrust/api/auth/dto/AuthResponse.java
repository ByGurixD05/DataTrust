package com.datatrust.api.auth.dto;

import java.time.Instant;
import java.util.UUID;

import com.datatrust.application.ports.AccessTokenIssuer;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UUID userId) {

    public static AuthResponse from(AccessTokenIssuer.IssuedAccessToken token) {
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), token.userId());
    }
}

package com.datatrust.infrastructure.security;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.datatrust.application.ports.AccessTokenIssuer;
import com.datatrust.domain.users.User;

@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration expiration;

    public JwtAccessTokenIssuer(
            JwtEncoder encoder,
            @Value("${app.security.jwt.issuer}") String issuer,
            @Value("${app.security.jwt.expiration}") Duration expiration) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expiration = expiration;
    }

    @Override
    public IssuedAccessToken issue(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expiration);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.id().toString())
                .claim("username", user.username())
                .claim("roles", List.of(user.role().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedAccessToken(token, expiresAt, user.id());
    }
}

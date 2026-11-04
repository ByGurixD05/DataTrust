package com.datatrust.infrastructure.security;

import java.util.UUID;

import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import com.datatrust.application.ports.CurrentUserProvider;
import com.datatrust.domain.enums.UserRole;

@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UUID currentUserId() {
        return parseUserId(authenticatedJwt().getSubject());
    }

    @Override
    public UserRole currentUserRole() {
        var roles = authenticatedJwt().getClaimAsStringList("roles");
        if (roles == null || roles.size() != 1) {
            throw new IllegalStateException("El token autenticado no contiene un rol válido");
        }
        try {
            return UserRole.valueOf(roles.get(0));
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("El token autenticado no contiene un rol válido", ex);
        }
    }

    private Jwt authenticatedJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationCredentialsNotFoundException(
                    "Se requiere un usuario autenticado");
        }
        return jwt;
    }

    private static UUID parseUserId(String subject) {
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("El token autenticado no contiene un UUID válido", ex);
        }
    }
}

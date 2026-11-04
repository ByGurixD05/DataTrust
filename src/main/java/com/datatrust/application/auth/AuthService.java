package com.datatrust.application.auth;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.datatrust.application.ports.AccessTokenIssuer;
import com.datatrust.application.ports.PasswordHasher;
import com.datatrust.application.ports.UserRepository;
import com.datatrust.domain.enums.UserRole;
import com.datatrust.domain.users.User;

@Service
public class AuthService {

    private static final int MIN_PASSWORD_LENGTH = 12;
    private static final int MAX_BCRYPT_PASSWORD_BYTES = 72;

    private final UserRepository users;
    private final PasswordHasher passwords;
    private final AccessTokenIssuer tokens;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordHasher passwords, AccessTokenIssuer tokens) {
        this.users = users;
        this.passwords = passwords;
        this.tokens = tokens;
        this.dummyHash = passwords.hash(UUID.randomUUID().toString());
    }

    @Transactional
    public AccessTokenIssuer.IssuedAccessToken register(
            String username, String password, String displayName, String email) {
        validatePassword(password);
        User user = User.create(UUID.randomUUID(), username, passwords.hash(password),
                UserRole.DATA_STEWARD, displayName, email);
        users.save(user);
        return tokens.issue(user);
    }

    @Transactional(readOnly = true)
    public AccessTokenIssuer.IssuedAccessToken login(String username, String password) {
        if (username == null || password == null) {
            throw new InvalidCredentialsException();
        }

        var found = users.findByUsername(username.strip());
        if (found.isEmpty()) {
            passwords.matches(password, dummyHash);
            throw new InvalidCredentialsException();
        }

        User user = found.get();
        if (!user.active() || !passwords.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }
        return tokens.issue(user);
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new InvalidPasswordException(
                    "La contraseña debe tener al menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_PASSWORD_BYTES) {
            throw new InvalidPasswordException(
                    "La contraseña no puede superar 72 bytes en UTF-8");
        }
    }
}

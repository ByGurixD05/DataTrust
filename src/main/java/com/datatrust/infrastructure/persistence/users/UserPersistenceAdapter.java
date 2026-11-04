package com.datatrust.infrastructure.persistence.users;

import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.datatrust.application.auth.DuplicateUserException;
import com.datatrust.application.ports.UserRepository;
import com.datatrust.domain.users.User;

@Repository
public class UserPersistenceAdapter implements UserRepository {

    private final UserJpaRepository jpa;

    public UserPersistenceAdapter(UserJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public void save(User user) {
        try {
            jpa.saveAndFlush(new UserEntity(user));
        } catch (DataIntegrityViolationException ex) {
            String field = duplicateField(ex);
            if (field != null) {
                throw new DuplicateUserException(field);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return jpa.findByUsernameIgnoreCase(username).map(UserEntity::toDomain);
    }

    private static String duplicateField(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.contains("ux_users_username_lower")) {
                return "username";
            }
            if (message != null && message.contains("ux_users_email_lower")) {
                return "email";
            }
        }
        return null;
    }
}

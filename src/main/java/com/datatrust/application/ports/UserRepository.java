package com.datatrust.application.ports;

import java.util.Optional;

import com.datatrust.domain.users.User;

public interface UserRepository {
    void save(User user);
    Optional<User> findByUsername(String username);
}

package com.datatrust.application.ports;

public interface PasswordHasher {
    String hash(String password);
    boolean matches(String password, String passwordHash);
}

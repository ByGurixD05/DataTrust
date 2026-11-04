package com.datatrust.application.auth;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Usuario o contraseña incorrectos");
    }
}

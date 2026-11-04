package com.datatrust.application.auth;

public class DuplicateUserException extends RuntimeException {

    private final String field;

    public DuplicateUserException(String field) {
        super("Ya existe un usuario con ese " + field);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

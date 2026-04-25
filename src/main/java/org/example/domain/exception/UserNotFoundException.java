package org.example.domain.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String userId) {
        super("No se encontró el usuario con id: " + userId);
    }
}
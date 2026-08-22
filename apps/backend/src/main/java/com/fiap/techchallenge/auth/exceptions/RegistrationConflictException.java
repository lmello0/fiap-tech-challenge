package com.fiap.techchallenge.auth.exceptions;

public class RegistrationConflictException extends RuntimeException {
    public RegistrationConflictException() {
        super("Registration conflicts with an existing account");
    }
}

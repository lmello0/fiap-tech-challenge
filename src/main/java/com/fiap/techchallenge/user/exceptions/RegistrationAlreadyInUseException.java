package com.fiap.techchallenge.user.exceptions;

public class RegistrationAlreadyInUseException extends RuntimeException {
    public RegistrationAlreadyInUseException(String registration) {
        super("Registration already in use: " + registration);
    }
}

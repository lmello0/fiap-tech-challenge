package com.fiap.techchallenge.user.exceptions;

public class EmailAlreadyInUseException extends RuntimeException {
    public EmailAlreadyInUseException() {
        super("Email already in use");
    }
}

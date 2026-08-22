package com.fiap.techchallenge.auth.exceptions;

public class AccountDisabledException extends RuntimeException {
    public AccountDisabledException(String message) {
        super(message);
    }
}

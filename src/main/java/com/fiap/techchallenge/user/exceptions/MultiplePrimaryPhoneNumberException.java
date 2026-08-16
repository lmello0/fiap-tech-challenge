package com.fiap.techchallenge.user.exceptions;

public class MultiplePrimaryPhoneNumberException extends RuntimeException {
    public MultiplePrimaryPhoneNumberException() {
        super("Only one primary phone number is allowed");
    }
}

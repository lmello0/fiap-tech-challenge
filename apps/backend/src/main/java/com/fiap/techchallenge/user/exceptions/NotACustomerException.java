package com.fiap.techchallenge.user.exceptions;

import java.util.UUID;

public class NotACustomerException extends RuntimeException {
    public NotACustomerException(UUID userId) {
        super("User is not a customer: " + userId);
    }
}

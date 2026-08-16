package com.fiap.techchallenge.user.exceptions;

import java.util.UUID;

public class NotAWorkerException extends RuntimeException {
    public NotAWorkerException(UUID userId) {
        super("User is not a worker: " + userId);
    }
}

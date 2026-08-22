package com.fiap.techchallenge.user.exceptions;

public class DocumentAlreadyInUseException extends RuntimeException {
    public DocumentAlreadyInUseException() {
        super("Document already in use");
    }
}

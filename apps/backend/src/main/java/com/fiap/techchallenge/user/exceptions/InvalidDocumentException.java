package com.fiap.techchallenge.user.exceptions;

import com.fiap.techchallenge.user.enums.DocumentType;

public class InvalidDocumentException extends RuntimeException {
    public InvalidDocumentException(DocumentType type) {
        super("Invalid " + type + " document code");
    }

    public InvalidDocumentException(String message) {
        super(message);
    }
}

package com.fiap.techchallenge.user.exceptions;

import com.fiap.techchallenge.user.enums.DocumentType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidDocumentExceptionTest {

    @Test
    void theDocumentTypeConstructorBuildsAMessageNamingTheType() {
        InvalidDocumentException exception = new InvalidDocumentException(DocumentType.CNPJ);

        assertThat(exception.getMessage()).isEqualTo("Invalid CNPJ document code");
    }

    @Test
    void theMessageConstructorUsesTheMessageAsIs() {
        InvalidDocumentException exception = new InvalidDocumentException("custom message");

        assertThat(exception.getMessage()).isEqualTo("custom message");
    }
}

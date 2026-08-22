package com.fiap.techchallenge.auth.exceptions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InvalidTokenExceptionTest {

    @Test
    void carriesJustAMessageWhenNoCauseIsGiven() {
        InvalidTokenException exception = new InvalidTokenException("Refresh token expired");

        assertThat(exception.getMessage()).isEqualTo("Refresh token expired");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void carriesTheOriginalCauseWhenGiven() {
        Throwable cause = new IllegalStateException("boom");

        InvalidTokenException exception = new InvalidTokenException("Token could not be validated", cause);

        assertThat(exception.getMessage()).isEqualTo("Token could not be validated");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}

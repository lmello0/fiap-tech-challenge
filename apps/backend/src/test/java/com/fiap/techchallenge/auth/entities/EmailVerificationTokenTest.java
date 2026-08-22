package com.fiap.techchallenge.auth.entities;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailVerificationTokenTest {

    @Test
    void isExpiredOnceThePastExpiryTimeIsReached() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isExpired(Instant.now())).isTrue();
    }

    @Test
    void isNotExpiredBeforeItsExpiryTime() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isExpired(Instant.now())).isFalse();
    }

    @Test
    void isUsedOnlyAfterBeingMarkedUsed() {
        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isUsed()).isFalse();

        token.markUsed(Instant.now());

        assertThat(token.isUsed()).isTrue();
    }
}

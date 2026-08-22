package com.fiap.techchallenge.auth.entities;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetTokenTest {

    @Test
    void isExpiredOnceThePastExpiryTimeIsReached() {
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isExpired(Instant.now())).isTrue();
    }

    @Test
    void isNotExpiredBeforeItsExpiryTime() {
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isExpired(Instant.now())).isFalse();
    }

    @Test
    void isUsedOnlyAfterBeingMarkedUsed() {
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isUsed()).isFalse();

        token.markUsed(Instant.now());

        assertThat(token.isUsed()).isTrue();
    }
}

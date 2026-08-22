package com.fiap.techchallenge.auth.entities;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmailChangeTokenTest {

    @Test
    void isExpiredOnceThePastExpiryTimeIsReached() {
        EmailChangeToken token = EmailChangeToken.builder()
                .userId(UUID.randomUUID())
                .newEmail("new@example.com")
                .tokenHash("hash")
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isExpired(Instant.now())).isTrue();
    }

    @Test
    void isNotExpiredBeforeItsExpiryTime() {
        EmailChangeToken token = EmailChangeToken.builder()
                .userId(UUID.randomUUID())
                .newEmail("new@example.com")
                .tokenHash("hash")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isExpired(Instant.now())).isFalse();
    }

    @Test
    void isUsedOnlyAfterBeingMarkedUsed() {
        EmailChangeToken token = EmailChangeToken.builder()
                .userId(UUID.randomUUID())
                .newEmail("new@example.com")
                .tokenHash("hash")
                .expiresAt(Instant.now().plus(Duration.ofMinutes(1)))
                .build();

        assertThat(token.isUsed()).isFalse();

        token.markUsed(Instant.now());

        assertThat(token.isUsed()).isTrue();
    }
}

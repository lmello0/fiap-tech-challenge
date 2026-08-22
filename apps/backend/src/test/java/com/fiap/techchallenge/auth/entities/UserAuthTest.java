package com.fiap.techchallenge.auth.entities;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserAuthTest {

    @Test
    void isNotLockedWhenNeverLocked() {
        UserAuth credential = UserAuth.local(UUID.randomUUID(), "hash");

        assertThat(credential.isLocked(Instant.now())).isFalse();
    }

    @Test
    void isNotLockedOnceTheLockWindowHasPassed() {
        UserAuth credential = UserAuth.local(UUID.randomUUID(), "hash");
        Instant now = Instant.now();

        credential.registerFailedAttempt(now.minus(Duration.ofHours(1)), 1, Duration.ofMinutes(15));

        assertThat(credential.isLocked(now)).isFalse();
    }

    @Test
    void isLockedWhileWithinTheLockWindow() {
        UserAuth credential = UserAuth.local(UUID.randomUUID(), "hash");
        Instant now = Instant.now();

        credential.registerFailedAttempt(now, 1, Duration.ofMinutes(15));

        assertThat(credential.isLocked(now)).isTrue();
    }

    @Test
    void doesNotLockBeforeReachingTheMaxAttempts() {
        UserAuth credential = UserAuth.local(UUID.randomUUID(), "hash");
        Instant now = Instant.now();

        credential.registerFailedAttempt(now, 3, Duration.ofMinutes(15));

        assertThat(credential.isLocked(now)).isFalse();
    }
}

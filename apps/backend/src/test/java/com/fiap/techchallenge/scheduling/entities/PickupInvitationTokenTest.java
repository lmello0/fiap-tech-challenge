package com.fiap.techchallenge.scheduling.entities;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PickupInvitationTokenTest {

    @Test
    void isUsedReflectsWhetherUsedAtIsSet() {
        PickupInvitationToken token = aToken();

        assertThat(token.isUsed()).isFalse();

        token.markUsed(Instant.now());

        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void isExpiredComparesAgainstExpiresAt() {
        Instant now = Instant.now();
        PickupInvitationToken expired = aToken(now.minus(1, ChronoUnit.MINUTES));
        PickupInvitationToken stillValid = aToken(now.plus(1, ChronoUnit.MINUTES));

        assertThat(expired.isExpired(now)).isTrue();
        assertThat(stillValid.isExpired(now)).isFalse();
    }

    private PickupInvitationToken aToken() {
        return aToken(Instant.now().plusSeconds(3600));
    }

    private PickupInvitationToken aToken(Instant expiresAt) {
        return PickupInvitationToken.builder()
                .workOrderId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .vehicleId(UUID.randomUUID())
                .tokenHash("hash")
                .expiresAt(expiresAt)
                .build();
    }
}

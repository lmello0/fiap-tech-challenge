package com.fiap.techchallenge.scheduling;

import com.fiap.techchallenge.scheduling.entities.AppointmentAccessToken;
import com.fiap.techchallenge.scheduling.entities.AppointmentRegistrationToken;
import com.fiap.techchallenge.scheduling.exceptions.InvalidAppointmentTokenException;
import com.fiap.techchallenge.scheduling.properties.SchedulingProperties;
import com.fiap.techchallenge.scheduling.repositories.AppointmentAccessTokenRepository;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRegistrationTokenRepository;
import com.fiap.techchallenge.scheduling.repositories.PickupInvitationTokenRepository;
import com.fiap.techchallenge.scheduling.services.AppointmentTokenService;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AppointmentTokenServiceTest {

    private final AppointmentAccessTokenRepository accessTokenRepository = mock(AppointmentAccessTokenRepository.class);
    private final AppointmentRegistrationTokenRepository registrationTokenRepository =
            mock(AppointmentRegistrationTokenRepository.class);
    private final PickupInvitationTokenRepository pickupInvitationTokenRepository =
            mock(PickupInvitationTokenRepository.class);

    private final SchedulingProperties properties = new SchedulingProperties(
            Duration.ofHours(2), Duration.ofDays(30), Duration.ofDays(7), Duration.ofDays(7), Duration.ofDays(14));

    private final AppointmentTokenService service = new AppointmentTokenService(
            accessTokenRepository, registrationTokenRepository, pickupInvitationTokenRepository, properties);

    @Test
    void issuedAccessTokenResolvesBackToItsAppointment() {
        UUID appointmentId = UUID.randomUUID();

        String[] savedHash = new String[1];
        when(accessTokenRepository.save(any())).thenAnswer(invocation -> {
            AppointmentAccessToken token = invocation.getArgument(0);
            savedHash[0] = token.getTokenHash();
            return token;
        });

        String raw = service.issueAccessToken(appointmentId);

        AppointmentAccessToken persisted = AppointmentAccessToken.builder()
                .appointmentId(appointmentId)
                .tokenHash(savedHash[0])
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .build();
        when(accessTokenRepository.findByTokenHash(savedHash[0])).thenReturn(Optional.of(persisted));

        assertThat(service.resolveAccessToken(raw)).isEqualTo(appointmentId);
    }

    @Test
    void rejectsAnUnknownAccessToken() {
        when(accessTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveAccessToken("not-a-real-token"))
                .isInstanceOf(InvalidAppointmentTokenException.class);
    }

    @Test
    void rejectsAnExpiredAccessToken() {
        AppointmentAccessToken expired = AppointmentAccessToken.builder()
                .appointmentId(UUID.randomUUID())
                .tokenHash("whatever")
                .expiresAt(Instant.now().minus(Duration.ofMinutes(1)))
                .build();
        when(accessTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resolveAccessToken("expired-token"))
                .isInstanceOf(InvalidAppointmentTokenException.class);
    }

    @Test
    void aConsumedRegistrationTokenCannotBeUsedAgain() {
        UUID appointmentId = UUID.randomUUID();
        AppointmentRegistrationToken token = AppointmentRegistrationToken.builder()
                .appointmentId(appointmentId)
                .tokenHash("some-hash")
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .build();

        when(registrationTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(token));

        assertThat(service.consumeRegistrationToken("raw")).isEqualTo(appointmentId);
        assertThat(token.isUsed()).isTrue();

        // Second attempt: the repository would now return the same (used) row.
        assertThatThrownBy(() -> service.consumeRegistrationToken("raw"))
                .isInstanceOf(InvalidAppointmentTokenException.class);
    }
}

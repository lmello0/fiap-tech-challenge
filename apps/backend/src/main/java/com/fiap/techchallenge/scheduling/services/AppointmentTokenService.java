package com.fiap.techchallenge.scheduling.services;

import com.fiap.techchallenge.scheduling.entities.AppointmentAccessToken;
import com.fiap.techchallenge.scheduling.entities.AppointmentRegistrationToken;
import com.fiap.techchallenge.scheduling.entities.PickupInvitationToken;
import com.fiap.techchallenge.scheduling.exceptions.InvalidAppointmentTokenException;
import com.fiap.techchallenge.scheduling.properties.SchedulingProperties;
import com.fiap.techchallenge.scheduling.repositories.AppointmentAccessTokenRepository;
import com.fiap.techchallenge.scheduling.repositories.AppointmentRegistrationTokenRepository;
import com.fiap.techchallenge.scheduling.repositories.PickupInvitationTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Generates/hashes/verifies the three guest token types (CONTEXT.md), identical mechanics to
 * {@code auth.services.EmailVerificationService}: a 32-byte URL-safe raw token, only its SHA-256 hash
 * persisted, the raw value held only in memory long enough to hand to the caller.
 */
@Service
@RequiredArgsConstructor
public class AppointmentTokenService {

    private final StringKeyGenerator tokenGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 32);

    private final AppointmentAccessTokenRepository accessTokenRepository;
    private final AppointmentRegistrationTokenRepository registrationTokenRepository;
    private final PickupInvitationTokenRepository pickupInvitationTokenRepository;
    private final SchedulingProperties properties;

    public String issueAccessToken(UUID appointmentId) {
        String raw = tokenGenerator.generateKey();

        accessTokenRepository.save(AppointmentAccessToken.builder()
                .appointmentId(appointmentId)
                .tokenHash(hash(raw))
                .expiresAt(Instant.now().plus(properties.accessTokenTtl()))
                .build());

        return raw;
    }

    public UUID resolveAccessToken(String rawToken) {
        Instant now = Instant.now();

        AppointmentAccessToken token = accessTokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> !t.isExpired(now))
                .orElseThrow(() -> new InvalidAppointmentTokenException("Invalid or expired appointment token"));

        return token.getAppointmentId();
    }

    public String issueRegistrationToken(UUID appointmentId) {
        String raw = tokenGenerator.generateKey();

        registrationTokenRepository.save(AppointmentRegistrationToken.builder()
                .appointmentId(appointmentId)
                .tokenHash(hash(raw))
                .expiresAt(Instant.now().plus(properties.registrationTokenTtl()))
                .build());

        return raw;
    }

    public UUID consumeRegistrationToken(String rawToken) {
        Instant now = Instant.now();

        AppointmentRegistrationToken token = registrationTokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> !t.isUsed() && !t.isExpired(now))
                .orElseThrow(() -> new InvalidAppointmentTokenException("Invalid or expired registration token"));

        token.markUsed(now);
        return token.getAppointmentId();
    }

    public String issuePickupInvitationToken(UUID workOrderId, UUID customerId, UUID vehicleId) {
        String raw = tokenGenerator.generateKey();

        pickupInvitationTokenRepository.save(PickupInvitationToken.builder()
                .workOrderId(workOrderId)
                .customerId(customerId)
                .vehicleId(vehicleId)
                .tokenHash(hash(raw))
                .expiresAt(Instant.now().plus(properties.pickupInvitationTokenTtl()))
                .build());

        return raw;
    }

    public PickupInvitationToken consumePickupInvitationToken(String rawToken) {
        Instant now = Instant.now();

        PickupInvitationToken token = pickupInvitationTokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> !t.isUsed() && !t.isExpired(now))
                .orElseThrow(() -> new InvalidAppointmentTokenException("Invalid or expired pickup invitation token"));

        token.markUsed(now);
        return token;
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}

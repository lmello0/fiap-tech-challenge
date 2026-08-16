package com.fiap.techchallenge.auth.services;

import com.fiap.techchallenge.auth.entities.EmailChangeToken;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.shared.notifications.EmailSender;
import com.fiap.techchallenge.auth.properties.VerificationProperties;
import com.fiap.techchallenge.auth.repositories.EmailChangeTokenRepository;
import com.fiap.techchallenge.user.api.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Orchestrates the email change flow (see ADR 0002 and ADR 0003).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailChangeService {

    private final StringKeyGenerator tokenGenerator = new Base64StringKeyGenerator(32);

    private final EmailChangeTokenRepository emailChangeTokenRepository;
    private final UserService userService;
    private final EmailSender emailSender;
    private final RefreshTokenService refreshTokenService;
    private final VerificationProperties properties;

    @Transactional
    public void requestEmailChange(UUID userId, String newEmail) {
        Instant now = Instant.now();
        emailChangeTokenRepository.invalidatePendingForUser(userId, now);

        String raw = tokenGenerator.generateKey();
        emailChangeTokenRepository.save(EmailChangeToken.builder()
                .userId(userId)
                .newEmail(newEmail)
                .tokenHash(hash(raw))
                .expiresAt(now.plus(properties.emailChangeTTL()))
                .build());

        emailSender.sendEmailChange(newEmail, raw);
    }

    @Transactional
    public void confirmEmailChange(String rawToken) {
        Instant now = Instant.now();

        EmailChangeToken token = emailChangeTokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> !t.isUsed() && !t.isExpired(now))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired email change token"));

        token.markUsed(now);
        userService.changeEmail(token.getUserId(), token.getNewEmail());
        refreshTokenService.revokeAllForUser(token.getUserId());

        log.info("Email changed userId={}", token.getUserId());
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

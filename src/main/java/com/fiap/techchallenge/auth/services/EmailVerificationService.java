package com.fiap.techchallenge.auth.services;

import com.fiap.techchallenge.auth.entities.EmailVerificationToken;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.notifications.AuthEmails;
import com.fiap.techchallenge.auth.properties.VerificationProperties;
import com.fiap.techchallenge.auth.repositories.EmailVerificationTokenRepository;
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
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Orchestrates the registration email verification flow (see ADR 0002 and ADR 0003).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final StringKeyGenerator tokenGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 32);

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserService userService;
    private final AuthEmails authEmails;
    private final VerificationProperties properties;

    @Transactional
    public void issueEmailVerification(UUID userId, String email) {
        Instant now = Instant.now();
        emailVerificationTokenRepository.invalidatePendingForUser(userId, now);

        String raw = tokenGenerator.generateKey();
        emailVerificationTokenRepository.save(EmailVerificationToken.builder()
                .userId(userId)
                .tokenHash(hash(raw))
                .expiresAt(now.plus(properties.emailVerificationTTL()))
                .build());

        authEmails.emailVerification(email, raw);
    }

    @Transactional
    public void resendEmailVerification(String email) {
        userService.findByEmail(email)
                .filter(user -> !user.emailVerified())
                .ifPresent(user -> issueEmailVerification(user.id(), email));

        log.info("Email verification resend requested");
    }

    @Transactional
    public void confirmEmailVerification(String rawToken) {
        Instant now = Instant.now();

        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> !t.isUsed() && !t.isExpired(now))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired email verification token"));

        token.markUsed(now);
        userService.markEmailVerified(token.getUserId());

        log.info("Email verified userId={}", token.getUserId());
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

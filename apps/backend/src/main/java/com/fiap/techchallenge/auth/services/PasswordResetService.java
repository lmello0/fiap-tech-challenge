package com.fiap.techchallenge.auth.services;

import com.fiap.techchallenge.auth.entities.PasswordResetToken;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.enums.AuthProvider;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.notifications.AuthEmails;
import com.fiap.techchallenge.auth.properties.VerificationProperties;
import com.fiap.techchallenge.auth.repositories.PasswordResetTokenRepository;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
import com.fiap.techchallenge.shared.logging.LogContext;
import com.fiap.techchallenge.user.api.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Orchestrates the password reset flow (see ADR 0002 and ADR 0003).
 */
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final StringKeyGenerator tokenGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 32);

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserAuthRepository userAuthRepository;
    private final UserService userService;
    private final AuthEmails authEmails;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;
    private final VerificationProperties properties;

    @Transactional
    public void requestPasswordReset(String email) {
        userService.findByEmail(email).ifPresent(user -> {
            Instant now = Instant.now();
            passwordResetTokenRepository.invalidatePendingForUser(user.id(), now);

            String raw = tokenGenerator.generateKey();
            passwordResetTokenRepository.save(PasswordResetToken.builder()
                    .userId(user.id())
                    .tokenHash(hash(raw))
                    .expiresAt(now.plus(properties.passwordResetTTL()))
                    .build());

            authEmails.passwordReset(email, raw);
        });
    }

    @Transactional
    public void confirmPasswordReset(String rawToken, String newPassword) {
        Instant now = Instant.now();

        PasswordResetToken token = passwordResetTokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> !t.isUsed() && !t.isExpired(now))
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token"));

        UserAuth credential = userAuthRepository
                .findByUserIdAndProvider(token.getUserId(), AuthProvider.LOCAL)
                .orElseThrow(() -> new InvalidTokenException("No local credential for this account"));

        credential.changePassword(passwordEncoder.encode(newPassword));
        token.markUsed(now);
        refreshTokenService.revokeAllForUser(token.getUserId());

        LogContext.put("userId", token.getUserId());
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

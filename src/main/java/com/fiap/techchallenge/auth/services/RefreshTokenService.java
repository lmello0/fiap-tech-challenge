package com.fiap.techchallenge.auth.services;

import com.fiap.techchallenge.auth.entities.RefreshToken;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.properties.JwtProperties;
import com.fiap.techchallenge.auth.repositories.RefreshTokenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;


@Slf4j
@Service
public class RefreshTokenService {

    private final StringKeyGenerator tokenGenerator = new Base64StringKeyGenerator(32);

    private final RefreshTokenRepository repository;
    private final JwtProperties properties;
    private final TransactionTemplate requiresNewTx;

    public RefreshTokenService(
            RefreshTokenRepository repository,
            JwtProperties properties,
            PlatformTransactionManager transactionManager
    ) {
        this.repository = repository;
        this.properties = properties;

        this.requiresNewTx = new TransactionTemplate(transactionManager);
        this.requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Transactional
    public String issue(UUID userId) {
        String raw = tokenGenerator.generateKey();
        Instant now = Instant.now();

        repository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(raw))
                .issuedAt(now)
                .expiresAt(now.plus(properties.refreshTokenTTL()))
                .build()
        );

        return raw;
    }

    @Transactional
    public Rotation rotate(String rawToken) {
        RefreshToken current = repository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new InvalidTokenException("Unknown refresh token"));

        Instant now = Instant.now();

        if (current.isRevoked()) {
            if (current.wasRotated()) {
                UUID userId = current.getUserId();

                // The token was already spent on a rotation, yet somebody still holds a working
                // copy: treat it as theft and kill every session for the user. This runs in its own
                // transaction because the exception below rolls the surrounding one back.
                requiresNewTx.executeWithoutResult(status -> repository.revokeAllForUser(userId, now));
                log.error("SECURITY: refresh-token reuse detected; revoked all sessions for userId={}", userId);

                throw new InvalidTokenException("Refresh token already used");
            }

            // Revoked by an explicit logout. A client retrying with a token it was told to drop is
            // not an attack, so the user's other sessions are left alone.
            throw new InvalidTokenException("Refresh token revoked");
        }

        if (current.isExpired(now)) {
            throw new InvalidTokenException("Refresh token expired");
        }

        String newRaw = tokenGenerator.generateKey();

        RefreshToken next = repository.save(RefreshToken.builder()
                .userId(current.getUserId())
                .tokenHash(hash(newRaw))
                .issuedAt(now)
                .expiresAt(now.plus(properties.refreshTokenTTL()))
                .build()
        );

        current.revoke(now, next.getId());
        log.debug("Refresh token rotated for userId={}", current.getUserId());

        return new Rotation(current.getUserId(), newRaw);
    }

    @Transactional
    public void revoke(String rawToken) {
        repository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (!token.isRevoked()) {
                token.revoke(Instant.now());
                log.info("Refresh token revoked for userId={}", token.getUserId());
            }
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        int revoked = repository.revokeAllForUser(userId, Instant.now());
        log.info("Revoked {} active refresh token(s) for userId={}", revoked, userId);
    }

    @Transactional
    public void deleteAllForUser(UUID userId) {
        repository.deleteByUserId(userId);
        log.info("Deleted all refresh tokens for userId={}", userId);
    }

    @Transactional
    public int purgeExpiredBefore(Instant cutoff) {
        return repository.deleteExpiredBefore(cutoff);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record Rotation(UUID userId, String newToken) {
    }
}

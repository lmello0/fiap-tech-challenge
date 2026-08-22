package com.fiap.techchallenge.auth;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.entities.RefreshToken;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.repositories.RefreshTokenRepository;
import com.fiap.techchallenge.auth.services.RefreshTokenService;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge cases of {@link RefreshTokenService} not already exercised end-to-end by
 * {@link AuthFlowIntegrationTest} (happy-path rotate/reuse-detection) or {@link RefreshTokenPurgeTest}
 * (purge).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RefreshTokenServiceTest {

    @Autowired
    RefreshTokenService service;

    @Autowired
    RefreshTokenRepository repository;

    @Autowired
    UserService userService;

    @Test
    void rotatingAnUnknownTokenIsRejected() {
        assertThatThrownBy(() -> service.rotate("not-a-real-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Unknown refresh token");
    }

    @Test
    void rotatingAnExpiredTokenIsRejected() {
        UUID userId = newUser();
        String raw = UUID.randomUUID().toString();

        repository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(hash(raw))
                .issuedAt(Instant.now().minus(Duration.ofDays(2)))
                .expiresAt(Instant.now().minus(Duration.ofDays(1)))
                .build());

        assertThatThrownBy(() -> service.rotate(raw))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Refresh token expired");
    }

    @Test
    void revokingAnAlreadyRevokedTokenIsANoOp() {
        UUID userId = newUser();
        String raw = service.issue(userId);

        service.revoke(raw);
        // second revoke must not blow up or double-log; it's just a no-op
        service.revoke(raw);

        UUID tokenId = repository.findByTokenHash(hash(raw)).orElseThrow().getId();
        assertThat(repository.findById(tokenId)).get()
                .extracting(RefreshToken::isRevoked)
                .isEqualTo(true);
    }

    @Test
    void deleteAllForUserRemovesEveryTokenForThatUser() {
        UUID userId = newUser();
        service.issue(userId);
        service.issue(userId);

        service.deleteAllForUser(userId);

        assertThat(repository.findAll())
                .noneMatch(token -> token.getUserId().equals(userId));
    }

    private UUID newUser() {
        String unique = UUID.randomUUID().toString().replace("-", "");

        return userService.createCustomer(new CreateUserCommand(
                unique.substring(0, 12) + "@example.com",
                "Ana",
                "Souza",
                DocumentType.CPF,
                uniqueDocument(),
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true))
        )).id();
    }

    /** Matches RefreshTokenService's own hashing so a manually-persisted token can be looked up. */
    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** DocumentValidator rejects bad check digits, so the digits have to be computed, not random. */
    private static String uniqueDocument() {
        int[] digits = new int[11];

        for (int i = 0; i < 9; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(10);
        }

        for (int position = 9; position < 11; position++) {
            int sum = 0;

            for (int i = 0; i < position; i++) {
                sum += digits[i] * (position + 1 - i);
            }

            int remainder = (sum * 10) % 11;
            digits[position] = remainder == 10 ? 0 : remainder;
        }

        StringBuilder cpf = new StringBuilder(11);
        for (int digit : digits) {
            cpf.append(digit);
        }

        return cpf.toString();
    }
}

package com.fiap.techchallenge.auth;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.entities.RefreshToken;
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

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RefreshTokenPurgeTest {

    @Autowired
    RefreshTokenService service;

    @Autowired
    RefreshTokenRepository repository;

    @Autowired
    UserService userService;

    @Test
    void purgesOnlyTokensPastTheCutoff() {
        UUID userId = newUser();

        UUID stale = persist(userId, Instant.now().minus(Duration.ofDays(90)));
        UUID recentlyExpired = persist(userId, Instant.now().minus(Duration.ofDays(1)));
        UUID live = persist(userId, Instant.now().plus(Duration.ofDays(1)));

        int deleted = service.purgeExpiredBefore(Instant.now().minus(Duration.ofDays(30)));

        assertThat(deleted).isEqualTo(1);
        assertThat(repository.findById(stale)).isEmpty();

        // still inside the retention window: kept so replaying them is recognised as reuse
        assertThat(repository.findById(recentlyExpired)).isPresent();
        assertThat(repository.findById(live)).isPresent();
    }

    private UUID persist(UUID userId, Instant expiresAt) {
        return repository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(UUID.randomUUID().toString().replace("-", "").repeat(2))
                .issuedAt(expiresAt.minus(Duration.ofHours(1)))
                .expiresAt(expiresAt)
                .build()
        ).getId();
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

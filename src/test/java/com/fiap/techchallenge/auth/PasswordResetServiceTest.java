package com.fiap.techchallenge.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.services.PasswordResetService;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Edge cases not already exercised end-to-end by {@link AuthEmailTokenFlowsTest} (happy-path
 * request/confirm via HTTP, invalid-token rejection via HTTP).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@RecordApplicationEvents
class PasswordResetServiceTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("reset-password\\?token=(\\S+)");

    @Autowired
    PasswordResetService service;

    @Autowired
    UserService userService;

    @Autowired
    ApplicationEvents events;

    final ObjectMapper json = new ObjectMapper();

    @Test
    void confirmingWithAnUnknownTokenIsRejected() {
        assertThatThrownBy(() -> service.confirmPasswordReset("not-a-real-token", "aNewPassword1"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired password reset token");
    }

    /**
     * A user created straight through {@code UserService} (bypassing {@code AuthServiceImpl}, as
     * several fixtures elsewhere in this module already do) has no LOCAL {@code UserAuth} row at
     * all. Requesting a reset doesn't check for one — only confirming does.
     */
    @Test
    void confirmingForAUserWithNoLocalCredentialIsRejected() {
        UserInfo user = newUser();

        service.requestPasswordReset(user.email());
        String token = tokenFor(user.email());

        assertThatThrownBy(() -> service.confirmPasswordReset(token, "aNewPassword1"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("No local credential for this account");
    }

    private String tokenFor(String email) {
        List<EmailRequestedEvent> published = events.stream(EmailRequestedEvent.class).toList();

        for (int i = published.size() - 1; i >= 0; i--) {
            if (!published.get(i).to().contains(email)) {
                continue;
            }

            Matcher matcher = TOKEN_PATTERN.matcher(published.get(i).plainText());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        throw new AssertionError("No password reset email found for " + email);
    }

    private UserInfo newUser() {
        String unique = UUID.randomUUID().toString().replace("-", "");

        return userService.createCustomer(new CreateUserCommand(
                unique.substring(0, 12) + "@example.com",
                "Ana",
                "Souza",
                DocumentType.CPF,
                uniqueDocument(),
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true))
        ));
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

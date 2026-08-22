package com.fiap.techchallenge.auth;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.repositories.EmailVerificationTokenRepository;
import com.fiap.techchallenge.auth.services.EmailVerificationService;
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

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmailVerificationServiceTest {

    @Autowired
    EmailVerificationService service;

    @Autowired
    UserService userService;

    @Autowired
    EmailVerificationTokenRepository tokens;

    @Test
    void confirmingWithAnUnknownTokenIsRejected() {
        assertThatThrownBy(() -> service.confirmEmailVerification("not-a-real-token"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired email verification token");
    }

    @Test
    void resendingForAnAlreadyVerifiedUserIsANoOp() {
        UserInfo user = newUser();
        userService.markEmailVerified(user.id());

        service.resendEmailVerification(user.email());

        assertThat(tokens.findAll())
                .noneMatch(token -> token.getUserId().equals(user.id()));
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

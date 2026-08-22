package com.fiap.techchallenge.auth;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.api.AuthService;
import com.fiap.techchallenge.auth.api.commands.ChangePasswordCommand;
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
import org.springframework.security.authentication.BadCredentialsException;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link AuthServiceImpl} edge cases reachable only by calling the service directly — not through
 * HTTP, since {@code /auth/password/change} requires a JWT that a user with no LOCAL credential could
 * never have obtained in the first place. A user created straight through {@code UserService}
 * (bypassing {@code AuthServiceImpl} entirely, as {@link AuthorizationTest}'s worker fixture already
 * does) has no {@code UserAuth} row at all.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class AuthServiceImplTest {

    @Autowired
    AuthService authService;

    @Autowired
    UserService userService;

    @Test
    void changingThePasswordForAUserWithNoLocalCredentialIsRejected() {
        UUID userId = newUser().id();

        assertThatThrownBy(() -> authService.changePassword(
                userId, new ChangePasswordCommand("anything", "aNewPassword12345")))
                .isInstanceOf(BadCredentialsException.class);
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

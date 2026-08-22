package com.fiap.techchallenge.user.services;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import com.fiap.techchallenge.user.enums.WorkerRole;
import com.fiap.techchallenge.user.exceptions.DocumentAlreadyInUseException;
import com.fiap.techchallenge.user.exceptions.EmailAlreadyInUseException;
import com.fiap.techchallenge.user.exceptions.InvalidDocumentException;
import com.fiap.techchallenge.user.exceptions.InvalidWorkerStartDateException;
import com.fiap.techchallenge.user.exceptions.MultiplePrimaryPhoneNumberException;
import com.fiap.techchallenge.user.exceptions.NotAWorkerException;
import com.fiap.techchallenge.user.exceptions.UserNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class UserServiceImplTest {

    @Autowired
    UserService userService;

    // --- createCustomer / newUser validation -----------------------------------------------------

    @Test
    void creatingACustomerWithTwoPrimaryPhoneNumbersIsRejected() {
        CreateUserCommand command = new CreateUserCommand(
                uniqueEmail(), "Ana", "Souza", DocumentType.CPF, uniqueDocument(),
                List.of(
                        new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11988880001", true),
                        new RegisterPhoneNumberCommand(PhoneType.HOME, "1133330002", true)
                )
        );

        assertThatThrownBy(() -> userService.createCustomer(command))
                .isInstanceOf(MultiplePrimaryPhoneNumberException.class);
    }

    @Test
    void creatingACustomerWithAnInvalidDocumentIsRejected() {
        CreateUserCommand command = new CreateUserCommand(
                uniqueEmail(), "Ana", "Souza", DocumentType.CPF, "00000000000",
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true))
        );

        assertThatThrownBy(() -> userService.createCustomer(command))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void creatingACustomerWithADocumentAlreadyInUseIsRejected() {
        String document = uniqueDocument();

        userService.createCustomer(new CreateUserCommand(
                uniqueEmail(), "Ana", "Souza", DocumentType.CPF, document,
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true))
        ));

        assertThatThrownBy(() -> userService.createCustomer(new CreateUserCommand(
                uniqueEmail(), "Beto", "Lima", DocumentType.CPF, document,
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11988888888", true))
        ))).isInstanceOf(DocumentAlreadyInUseException.class);
    }

    // --- createWorker ------------------------------------------------------------------------------

    @Test
    void creatingAWorkerWhoseStartDateIsBeforeTheirHireDateIsRejected() {
        CreateWorkerCommand command = new CreateWorkerCommand(
                userCommand(uniqueEmail()),
                WorkerRole.MECHANIC,
                LocalDate.now(),
                LocalDate.now().minusDays(1)
        );

        assertThatThrownBy(() -> userService.createWorker(command))
                .isInstanceOf(InvalidWorkerStartDateException.class);
    }

    // --- terminateWorker -----------------------------------------------------------------------------

    @Test
    void terminatingAUserWhoIsNotAWorkerIsRejected() {
        UserInfo customer = userService.createCustomer(userCommand(uniqueEmail()));

        assertThatThrownBy(() -> userService.terminateWorker(customer.id(), LocalDate.now()))
                .isInstanceOf(NotAWorkerException.class);
    }

    @Test
    void terminatingAnUnknownUserIsRejected() {
        assertThatThrownBy(() -> userService.terminateWorker(UUID.randomUUID(), LocalDate.now()))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- markEmailVerified / changeEmail -------------------------------------------------------------

    @Test
    void markingAnUnknownUsersEmailVerifiedIsRejected() {
        assertThatThrownBy(() -> userService.markEmailVerified(UUID.randomUUID()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void changingAnUnknownUsersEmailIsRejected() {
        assertThatThrownBy(() -> userService.changeEmail(UUID.randomUUID(), uniqueEmail()))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void changingEmailToOneAlreadyInUseIsRejected() {
        UserInfo first = userService.createCustomer(userCommand(uniqueEmail()));
        UserInfo second = userService.createCustomer(userCommand(uniqueEmail()));

        assertThatThrownBy(() -> userService.changeEmail(second.id(), first.email()))
                .isInstanceOf(EmailAlreadyInUseException.class);
    }

    @Test
    void changingEmailToAFreeAddressSucceeds() {
        UserInfo user = userService.createCustomer(userCommand(uniqueEmail()));
        String newEmail = uniqueEmail();

        userService.changeEmail(user.id(), newEmail);

        assertThat(userService.getById(user.id()).email()).isEqualTo(newEmail);
    }

    // --- helpers -------------------------------------------------------------------------------------

    private CreateUserCommand userCommand(String email) {
        return new CreateUserCommand(
                email, "Ana", "Souza", DocumentType.CPF, uniqueDocument(),
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true))
        );
    }

    private static String uniqueEmail() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
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

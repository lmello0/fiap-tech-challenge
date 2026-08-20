package com.fiap.techchallenge.auth.services;

import com.fiap.techchallenge.auth.api.AuthService;
import com.fiap.techchallenge.auth.api.commands.RegisterWorkerCommand;
import com.fiap.techchallenge.auth.properties.BootstrapManagerProperties;
import com.fiap.techchallenge.shared.logging.Masking;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import com.fiap.techchallenge.user.enums.WorkerRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapManagerRunner implements ApplicationRunner {

    private final StringKeyGenerator passwordGenerator = new Base64StringKeyGenerator(16);

    private final BootstrapManagerProperties properties;
    private final UserService userService;
    private final AuthService authService;

    @Override
    public void run(@Nullable ApplicationArguments args) {
        if (!StringUtils.hasText(properties.email())) {
            return;
        }

        if (userService.findByEmail(properties.email()).isPresent()) {
            log.info("Bootstrap manager already exists, skipping. email={}", Masking.email(properties.email()));
            return;
        }

        String rawPassword = properties.password() == null
                ? passwordGenerator.generateKey()
                : properties.password();

        CreateUserCommand user = new CreateUserCommand(
                properties.email(),
                properties.firstName(),
                properties.lastName(),
                DocumentType.valueOf(properties.documentType()),
                properties.documentCode(),
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, properties.phone(), true))
        );

        CreateWorkerCommand worker = new CreateWorkerCommand(
                user,
                WorkerRole.MANAGER,
                LocalDate.now(),
                LocalDate.now()
        );

        authService.registerWorker(new RegisterWorkerCommand(worker, rawPassword));

        log.warn(
                """
                        
                        {}
                        
                         Bootstrap MANAGER created — email={} password={} — this password is single-use: \
                        the account can do nothing but POST /auth/password/change until it is rotated. \
                        Unset the BOOTSTRAP_MANAGER_* env vars afterwards\s
                        
                        {}""",
                "=".repeat(30), properties.email(), rawPassword, "=".repeat(30)
        );
    }
}

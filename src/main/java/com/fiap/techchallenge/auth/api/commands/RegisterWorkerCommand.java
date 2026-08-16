package com.fiap.techchallenge.auth.api.commands;

import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterWorkerCommand(
        @Valid
        @NotNull(message = "Worker data may not be null")
        CreateWorkerCommand worker,

        @NotBlank(message = "Password may not be blank")
        @Size(min = 16, max = 72, message = "Password must be between 16 and 72 characters")
        String rawPassword
) {
}

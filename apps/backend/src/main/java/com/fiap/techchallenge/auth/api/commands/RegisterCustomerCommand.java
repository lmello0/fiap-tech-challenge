package com.fiap.techchallenge.auth.api.commands;

import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterCustomerCommand(
        @Valid
        @NotNull(message = "User data may not be null")
        CreateUserCommand user,

        @NotBlank(message = "Password may not be blank")
        @Size(min = 16, max = 72, message = "Password must be between 16 and 72 characters")
        String rawPassword
) {
}

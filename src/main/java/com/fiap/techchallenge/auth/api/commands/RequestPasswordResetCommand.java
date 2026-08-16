package com.fiap.techchallenge.auth.api.commands;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestPasswordResetCommand(
        @Email
        @NotBlank(message = "Email may not be blank")
        String email
) {
}

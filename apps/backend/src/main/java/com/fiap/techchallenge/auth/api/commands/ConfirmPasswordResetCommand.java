package com.fiap.techchallenge.auth.api.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmPasswordResetCommand(
        @NotBlank(message = "Token may not be blank")
        String token,

        @NotBlank(message = "Password may not be blank")
        @Size(min = 16, max = 72, message = "Password must be between 16 and 72 characters")
        String newPassword
) {
}

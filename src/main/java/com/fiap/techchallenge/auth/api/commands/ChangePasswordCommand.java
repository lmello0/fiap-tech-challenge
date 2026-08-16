package com.fiap.techchallenge.auth.api.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordCommand(
        @NotBlank(message = "Current password may not be blank")
        String currentPassword,

        @NotBlank(message = "New password may not be blank")
        @Size(min = 16, max = 72, message = "New password must be between 16 and 72 characters")
        String newPassword
) {
}

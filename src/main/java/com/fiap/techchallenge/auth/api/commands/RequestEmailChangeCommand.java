package com.fiap.techchallenge.auth.api.commands;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestEmailChangeCommand(
        @Email
        @NotBlank(message = "Email may not be blank")
        @Size(max = 255, message = "Email may not exceed 255 characters")
        String newEmail
) {
}

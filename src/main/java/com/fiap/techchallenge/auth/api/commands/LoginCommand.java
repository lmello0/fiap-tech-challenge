package com.fiap.techchallenge.auth.api.commands;

import jakarta.validation.constraints.NotBlank;

public record LoginCommand(
        @NotBlank(message = "Email may not be blank")
        String email,

        @NotBlank(message = "Password may not be blank")
        String rawPassword
) {
}

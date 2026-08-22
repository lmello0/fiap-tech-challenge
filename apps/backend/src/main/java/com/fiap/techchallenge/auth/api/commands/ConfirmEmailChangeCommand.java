package com.fiap.techchallenge.auth.api.commands;

import jakarta.validation.constraints.NotBlank;

public record ConfirmEmailChangeCommand(
        @NotBlank(message = "Token may not be blank")
        String token
) {
}

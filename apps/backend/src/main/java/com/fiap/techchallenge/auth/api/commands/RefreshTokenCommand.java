package com.fiap.techchallenge.auth.api.commands;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenCommand(
        @NotBlank(message = "Refresh token may not be blank")
        String refreshToken
) {
}

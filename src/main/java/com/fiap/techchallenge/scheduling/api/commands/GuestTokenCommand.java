package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.NotBlank;

public record GuestTokenCommand(
        @NotBlank(message = "Token may not be blank")
        String token
) {
}

package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record BookPickupByTokenCommand(
        @NotBlank(message = "Token may not be blank")
        String token,

        @NotNull(message = "Slot start may not be null")
        @Future(message = "Slot start must be in the future")
        Instant slotStart
) {
}

package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record RescheduleAppointmentCommand(
        @NotNull(message = "New slot start may not be null")
        @Future(message = "New slot start must be in the future")
        Instant newSlotStart
) {
}

package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public record BookPickupCommand(
        @NotNull(message = "Work order ID may not be null")
        UUID workOrderId,

        @NotNull(message = "Vehicle ID may not be null")
        UUID vehicleId,

        @NotNull(message = "Slot start may not be null")
        @Future(message = "Slot start must be in the future")
        Instant slotStart
) {
}

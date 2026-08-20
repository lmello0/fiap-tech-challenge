package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

public record UpdateSchedulingSettingsCommand(
        @NotNull(message = "Business start time may not be null")
        LocalTime businessStartTime,

        @NotNull(message = "Business end time may not be null")
        LocalTime businessEndTime,

        @Min(value = 1, message = "Drop-off slot capacity must be at least 1")
        int dropoffSlotCapacity,

        @Min(value = 1, message = "Pickup slot capacity must be at least 1")
        int pickupSlotCapacity
) {
}

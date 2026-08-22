package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;
import java.util.UUID;

public record BookCustomerDropoffCommand(
        @NotNull(message = "Vehicle ID may not be null")
        UUID vehicleId,

        @NotBlank(message = "Complaint may not be blank")
        @Length(max = 2000, message = "Complaint cannot exceed 2_000 characters")
        String complaint,

        @NotNull(message = "Slot start may not be null")
        @Future(message = "Slot start must be in the future")
        Instant slotStart
) {
}

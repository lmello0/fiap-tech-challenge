package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;
import java.util.UUID;

/**
 * An Attendant/Manager booking a Drop-off on someone's behalf: either {@code customerId} +
 * {@code vehicleId} for an existing registered customer, or the guest fields for a new contact —
 * exactly one of the two shapes, enforced in {@code AppointmentServiceImpl}.
 */
public record BookDropoffOnBehalfCommand(
        UUID customerId,
        UUID vehicleId,

        String guestName,
        String guestPhone,
        String guestEmail,
        String guestVehicleMake,
        String guestVehicleModel,
        Integer guestVehicleYear,

        @NotBlank(message = "Complaint may not be blank")
        @Length(max = 2000, message = "Complaint cannot exceed 2_000 characters")
        String complaint,

        @NotNull(message = "Slot start may not be null")
        @Future(message = "Slot start must be in the future")
        Instant slotStart
) {
}

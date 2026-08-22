package com.fiap.techchallenge.scheduling.api.commands;

import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Attendant-initiated Guest Conversion at Check-in (CONTEXT.md): no password field — the system
 * generates one and the Attendant relays it to the customer standing at the counter.
 */
public record RegisterGuestAtCheckInCommand(
        @NotNull(message = "Document type may not be null")
        DocumentType documentType,

        @NotBlank(message = "Document code may not be blank")
        @Size(max = 50, message = "Document code may not exceed 50 characters")
        String documentCode,

        @NotBlank(message = "License plate may not be blank")
        @Size(max = 7, message = "License plate may not exceed 7 characters")
        String licensePlate,

        @NotNull(message = "Vehicle type may not be null")
        VehicleType vehicleType,

        @NotBlank(message = "Color may not be blank")
        @Size(max = 30, message = "Color may not exceed 30 characters")
        String color,

        @NotNull(message = "Fuel type may not be null")
        FuelType fuelType,

        @NotNull(message = "Transmission type may not be null")
        TransmissionType transmissionType
) {
}

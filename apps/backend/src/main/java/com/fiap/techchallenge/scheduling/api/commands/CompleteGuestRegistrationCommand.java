package com.fiap.techchallenge.scheduling.api.commands;

import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Self-service Guest Conversion (CONTEXT.md): consumes the Complete-Registration Token and supplies
 * everything the original guest form deliberately left out — a chosen password, the document required
 * by {@code User}, and the vehicle fields {@code CreateVehicleCommand} requires beyond make/model/year
 * (plate, type, color, fuel, transmission).
 */
public record CompleteGuestRegistrationCommand(
        @NotBlank(message = "Token may not be blank")
        String token,

        @NotBlank(message = "Password may not be blank")
        @Size(min = 16, max = 72, message = "Password must be between 16 and 72 characters")
        String rawPassword,

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

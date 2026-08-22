package com.fiap.techchallenge.vehicle.api.commands;

import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.With;

import java.util.UUID;

public record CreateVehicleCommand(
        @With
        UUID customerId,

        @NotNull(message = "Vehicle type may not be null")
        VehicleType vehicleType,

        @NotBlank(message = "License plate may not be blank")
        @Size(max = 7, message = "License plate may not exceed 7 characters")
        String licensePlate,

        @NotBlank(message = "Make may not be blank")
        @Size(max = 50, message = "Make may not exceed 50 characters")
        String make,

        @NotBlank(message = "Model may not be blank")
        @Size(max = 100, message = "Model may not exceed 100 characters")
        String model,

        @NotBlank(message = "Color may not be blank")
        @Size(max = 30, message = "Color may not exceed 30 characters")
        String color,

        @NotNull(message = "Model year may not be null")
        Integer modelYear,

        Integer manufactureYear,

        @Size(max = 100, message = "Version may not exceed 100 characters")
        String version,

        @NotNull(message = "Fuel type may not be null")
        FuelType fuelType,

        @NotNull(message = "Transmission type may not be null")
        TransmissionType transmissionType
) {
}

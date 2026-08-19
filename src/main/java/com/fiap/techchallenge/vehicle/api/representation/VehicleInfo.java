package com.fiap.techchallenge.vehicle.api.representation;

import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;

import java.time.Instant;
import java.util.UUID;

public record VehicleInfo(
        UUID id,
        UUID customerId,
        VehicleType vehicleType,
        String licensePlate,
        String make,
        String model,
        String color,
        Integer modelYear,
        Integer manufactureYear,
        String version,
        FuelType fuelType,
        TransmissionType transmissionType,
        boolean active,
        Instant deactivatedAt,
        Instant createdAt,
        Instant updatedAt
) {
}

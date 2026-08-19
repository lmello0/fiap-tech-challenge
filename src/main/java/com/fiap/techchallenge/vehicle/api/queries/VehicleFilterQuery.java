package com.fiap.techchallenge.vehicle.api.queries;

import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import lombok.With;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VehicleFilterQuery(
    @With UUID customerId,
    List<VehicleType> vehicleTypes,
    String licensePlate,
    String make,
    String model,
    String color,
    Integer modelYearStart,
    Integer modelYearEnd,
    Integer manufactureYearStart,
    Integer manufactureYearEnd,
    List<FuelType> fuelTypes,
    List<TransmissionType> transmissionTypes,
    LocalDate createdAtStart,
    LocalDate createdAtEnd
) {
}

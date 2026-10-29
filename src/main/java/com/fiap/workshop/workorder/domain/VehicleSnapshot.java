package com.fiap.workshop.workorder.domain;

import java.util.UUID;

public record VehicleSnapshot(UUID id, String plate, String brand, String model, int year) {
}

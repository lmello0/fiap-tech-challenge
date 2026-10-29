package com.fiap.workshop.customer.api;

import java.util.UUID;

public record VehicleView(UUID id, UUID customerId, String plate, String brand, String model, int year, boolean active) {
}

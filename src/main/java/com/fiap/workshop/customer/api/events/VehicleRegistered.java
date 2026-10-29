package com.fiap.workshop.customer.api.events;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.customer::vehicle-registered")
public record VehicleRegistered(UUID eventId, Instant occurredAt, UUID vehicleId, UUID customerId, String plate, String brand, String model, int year) {
}

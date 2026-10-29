package com.fiap.workshop.inventory.api.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.inventory::reservation-released")
public record ReservationReleased(UUID eventId, Instant occurredAt, UUID workOrderId) {
}

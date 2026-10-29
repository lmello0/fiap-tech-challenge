package com.fiap.workshop.inventory.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.inventory::part-deactivated")
public record PartDeactivated(UUID eventId, Instant occurredAt, UUID partId) {
}

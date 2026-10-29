package com.fiap.workshop.inventory.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.inventory::part-created")
public record PartCreated(UUID eventId, Instant occurredAt, UUID partId, String sku, String name, BigDecimal unitPrice) {
}

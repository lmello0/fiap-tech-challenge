package com.fiap.workshop.catalog.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.catalog::service-created")
public record ServiceCreated(UUID eventId, Instant occurredAt, UUID serviceId, String name, BigDecimal price, int estimatedMinutes) {
}

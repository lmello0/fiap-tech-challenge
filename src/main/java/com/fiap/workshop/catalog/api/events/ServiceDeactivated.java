package com.fiap.workshop.catalog.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.catalog::service-deactivated")
public record ServiceDeactivated(UUID eventId, Instant occurredAt, UUID serviceId) {
}

package com.fiap.workshop.customer.api.events;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.customer::customer-deactivated")
public record CustomerDeactivated(UUID eventId, Instant occurredAt, UUID customerId) {
}

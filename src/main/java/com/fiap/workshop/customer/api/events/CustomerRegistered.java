package com.fiap.workshop.customer.api.events;

import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.customer::customer-registered")
public record CustomerRegistered(UUID eventId, Instant occurredAt, UUID customerId, String name, String document, String email, String phone) {
}

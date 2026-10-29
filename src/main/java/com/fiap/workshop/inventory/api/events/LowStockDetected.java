package com.fiap.workshop.inventory.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.inventory::low-stock-detected")
public record LowStockDetected(UUID eventId, Instant occurredAt, UUID partId, String sku, String name, int onHand, int minimumQuantity) {
}

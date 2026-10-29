package com.fiap.workshop.inventory.api.events;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.inventory::parts-shortage")
public record PartsShortage(UUID eventId, Instant occurredAt, UUID workOrderId, UUID budgetId, List<ShortagePart> shortages) {
}

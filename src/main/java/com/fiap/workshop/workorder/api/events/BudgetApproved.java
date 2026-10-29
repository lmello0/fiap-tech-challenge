package com.fiap.workshop.workorder.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.workorder::budget-approved")
public record BudgetApproved(UUID eventId, Instant occurredAt, UUID workOrderId, String number, UUID budgetId, BigDecimal total, List<PartQuantity> parts, String channel) {
}

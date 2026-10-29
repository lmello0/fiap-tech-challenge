package com.fiap.workshop.workorder.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.workorder::budget-issued")
public record BudgetIssued(UUID eventId, Instant occurredAt, UUID workOrderId, String number, UUID budgetId, int sequence, String kind, BigDecimal total, UUID customerId, String customerName, String customerEmail, List<BudgetLineData> lines) {
}

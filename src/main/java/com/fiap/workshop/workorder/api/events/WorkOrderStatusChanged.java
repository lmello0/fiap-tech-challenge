package com.fiap.workshop.workorder.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.workorder::work-order-status-changed")
public record WorkOrderStatusChanged(UUID eventId, Instant occurredAt, UUID workOrderId, String number, UUID customerId, String fromStatus, String toStatus, String actorSubject, String actorRole, String customerName, String customerEmail) {
}

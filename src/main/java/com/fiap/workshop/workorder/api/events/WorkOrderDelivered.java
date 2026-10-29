package com.fiap.workshop.workorder.api.events;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.modulith.events.Externalized;

@Externalized("workshop.workorder::work-order-delivered")
public record WorkOrderDelivered(UUID eventId, Instant occurredAt, UUID workOrderId, String number, UUID customerId) {
}

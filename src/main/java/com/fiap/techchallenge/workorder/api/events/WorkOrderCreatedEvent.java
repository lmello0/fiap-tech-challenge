package com.fiap.techchallenge.workorder.api.events;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderCreatedEvent(
        UUID workOrderId,
        String orderNumber,
        UUID customerId,
        UUID vehicleId,
        Instant occurredAt
) {
}

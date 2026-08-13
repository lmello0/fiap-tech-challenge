package com.fiap.techchallenge.workorder.api.events;

import java.util.UUID;

public record WorkOrderRefusedEvent(
        UUID workOrderId,
        UUID customerId,
        String reason
) {
}

package com.fiap.techchallenge.workorder.api.events;

import java.util.UUID;

public record WorkOrderFinishedEvent(
        UUID workOrderId,
        UUID vehicleId
) {
}

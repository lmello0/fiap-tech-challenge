package com.fiap.techchallenge.workorder.api.events;

import java.util.UUID;

public record WorkOrderWaitingPickupEvent(
        UUID workOrderId
) {
}

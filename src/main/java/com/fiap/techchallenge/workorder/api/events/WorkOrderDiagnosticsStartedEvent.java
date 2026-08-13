package com.fiap.techchallenge.workorder.api.events;

import java.util.UUID;

public record WorkOrderDiagnosticsStartedEvent(
        UUID workOrderId,
        UUID vehicleId,
        UUID mechanicId
) {
}

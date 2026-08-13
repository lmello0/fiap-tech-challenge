package com.fiap.techchallenge.workorder.api.events;

import java.util.UUID;

public record WorkOrderDiagnosticsRequestedEvent(
        UUID workOrderId,
        UUID vehicleId
) {
}

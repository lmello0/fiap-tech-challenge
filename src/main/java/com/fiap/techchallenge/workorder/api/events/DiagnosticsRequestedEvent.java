package com.fiap.techchallenge.workorder.api.events;

import java.util.UUID;

public record DiagnosticsRequestedEvent(
        UUID workOrderId,
        UUID vehicleId
) {
}

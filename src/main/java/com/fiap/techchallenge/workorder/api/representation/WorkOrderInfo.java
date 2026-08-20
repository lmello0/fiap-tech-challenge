package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderInfo(
        UUID id,
        String orderCode,
        WorkOrderStatus status,
        UUID customerId,
        UUID vehicleId,
        UUID assignedMechanicId,
        String customerComplaint,
        String diagnosis,
        String refusalReason,
        UUID budgetId,
        Instant createdAt,
        Instant updatedAt,
        Instant diagnosticRequestedAt,
        Instant diagnosticStartedAt,
        Instant diagnosticFinishedAt,
        Instant approvedAt,
        Instant refusedAt,
        Instant serviceStartedAt,
        Instant finishedAt,
        Instant pickupReadyAt,
        Instant deliveredAt
) {

}

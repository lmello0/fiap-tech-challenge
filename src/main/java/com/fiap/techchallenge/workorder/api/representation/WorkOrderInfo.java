package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
        List<WorkOrderRowInfo> rows,
        BigDecimal laborTotal,
        BigDecimal partsTotal,
        BigDecimal grandTotal,
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

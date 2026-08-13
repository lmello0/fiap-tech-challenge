package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.RowType;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkOrderInfo(
        UUID id,
        String orderNumber,
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
        BigDecimal discount,
        BigDecimal taxTotal,
        BigDecimal grandTotal,
        Instant createdAt,
        Instant updatedAt,
        Instant approvedAt,
        Instant refusedAt,
        Instant finishedAt,
        Instant deliveredAt
) {
    public record WorkOrderRowInfo(
            RowType type,
            String description,
            BigDecimal quantity,
            BigDecimal unitPrice,
            BigDecimal lineTotal,
            UUID partId
    ) {
    }
}

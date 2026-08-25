package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * One row of a customer's own work order list: just enough to recognize which car and how much,
 * without the full Budget lines {@link CustomerWorkOrderView} carries. CUSTOMER principals never see
 * {@link WorkOrderInfo}.
 */
public record CustomerWorkOrderSummary(
        UUID id,
        String orderCode,
        WorkOrderStatus status,
        UUID vehicleId,
        String vehiclePlate,
        String vehicleMake,
        String vehicleModel,
        BigDecimal budgetTotal,
        Instant createdAt
) {
}

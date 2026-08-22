package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;

import java.util.UUID;

/**
 * Narrow, customer-facing read of a work order: high-level status plus its Budget, and nothing else
 * internal (no assigned mechanic, no diagnosis notes). CUSTOMER principals never see {@link WorkOrderInfo}.
 */
public record CustomerWorkOrderView(
        UUID id,
        String orderCode,
        WorkOrderStatus status,
        BudgetInfo budget
) {
}

package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;

import java.util.UUID;

/**
 * Narrow, customer-facing read of a work order: high-level status, its vehicle, the diagnosis behind
 * the Budget, and the Budget itself — nothing else internal (no assigned mechanic). A customer
 * approving or refusing a Budget has a right to know why it's needed; that's what {@code diagnosis} is
 * for. CUSTOMER principals never see {@link WorkOrderInfo}.
 */
public record CustomerWorkOrderView(
        UUID id,
        String orderCode,
        WorkOrderStatus status,
        UUID vehicleId,
        String vehiclePlate,
        String vehicleMake,
        String vehicleModel,
        String diagnosis,
        BudgetInfo budget
) {
}

package com.fiap.techchallenge.workorder.enums;

public enum WorkOrderStatus {
    RECEIVED,
    WAITING_DIAGNOSTICS,
    IN_DIAGNOSTICS,
    BUDGET_IN_DRAFT,
    WAITING_APPROVAL,
    APPROVED,
    REFUSED,
    IN_PROGRESS,
    FINISHED,
    WAITING_PICKUP,
    DELIVERED,
    CANCELLED
}

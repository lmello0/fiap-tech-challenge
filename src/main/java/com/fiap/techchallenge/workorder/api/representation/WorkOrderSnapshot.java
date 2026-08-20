package com.fiap.techchallenge.workorder.api.representation;

/**
 * The Snapshot (CONTEXT.md) carried by every Work Order Timeline event — the order together with its
 * current Budget and that Budget's lines, since "what did we quote them" only answers anything when
 * the lines are included (ADR 0012, Q14).
 */
public record WorkOrderSnapshot(
        WorkOrderInfo workOrder,
        BudgetInfo currentBudget
) {
}

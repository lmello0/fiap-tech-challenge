package com.fiap.techchallenge.workorder.api.representation;

public record WorkOrderCountStatusInfo(
        Long received,
        Long waitingDiagnostics,
        Long inDiagnostics,
        Long budgetInDraft,
        Long waitingApproval,
        Long approved,
        Long refused,
        Long inProgress,
        Long finished,
        Long waitingPickup,
        Long delivered
) {
}

package com.fiap.techchallenge.workorder.api.events;

import java.util.UUID;

public record BudgetLineStartedEvent(
        UUID workOrderId,
        UUID budgetLineId
) {
}

package com.fiap.techchallenge.workorder.api.events;

import java.util.UUID;

public record BudgetLineFinishedEvent(
        UUID workOrderId,
        UUID budgetLineId
) {
}

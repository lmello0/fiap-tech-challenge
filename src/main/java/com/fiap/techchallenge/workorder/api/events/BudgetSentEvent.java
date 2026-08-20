package com.fiap.techchallenge.workorder.api.events;

import java.math.BigDecimal;
import java.util.UUID;

/** Published once delivery of the budget email is confirmed (Budget WAITING_SEND -> SENT). */
public record BudgetSentEvent(
        UUID workOrderId,
        UUID budgetId,
        UUID customerId,
        BigDecimal grandTotal
) {
}

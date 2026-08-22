package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.BudgetStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BudgetInfo(
        UUID id,
        UUID workOrderId,
        BudgetStatus status,
        List<BudgetLineInfo> lines,
        BigDecimal grandTotal,
        Instant createdAt,
        Instant sentAt,
        Instant resolvedAt
) {
}

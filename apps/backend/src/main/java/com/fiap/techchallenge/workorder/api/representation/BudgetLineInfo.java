package com.fiap.techchallenge.workorder.api.representation;

import com.fiap.techchallenge.workorder.enums.RowType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BudgetLineInfo(
        UUID id,
        RowType type,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal,
        UUID partId,
        UUID serviceId,
        Instant startedAt,
        Instant finishedAt
) {
}

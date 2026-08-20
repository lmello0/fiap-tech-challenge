package com.fiap.techchallenge.inventory.api.representation;

import com.fiap.techchallenge.inventory.enums.StockMovementType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockMovementInfo(
        UUID id,
        UUID partId,
        StockMovementType type,
        BigDecimal quantity,
        BigDecimal unitCost,
        UUID referenceId,
        String reason,
        Instant occurredAt
) {
}

package com.fiap.techchallenge.inventory.api.representation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record StockPolicyInfo(
        UUID id,
        UUID partId,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        UUID vendorId,
        boolean autoReorderEnabled,
        Instant createdAt,
        Instant updatedAt
) {
}

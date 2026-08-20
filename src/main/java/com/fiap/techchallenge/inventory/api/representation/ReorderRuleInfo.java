package com.fiap.techchallenge.inventory.api.representation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReorderRuleInfo(
        UUID id,
        UUID partId,
        BigDecimal minQuantity,
        BigDecimal maxQuantity,
        UUID vendorId,
        boolean enabled,
        Instant createdAt,
        Instant updatedAt
) {
}

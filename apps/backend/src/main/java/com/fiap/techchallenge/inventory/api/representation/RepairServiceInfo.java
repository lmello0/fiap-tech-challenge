package com.fiap.techchallenge.inventory.api.representation;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RepairServiceInfo(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal price,
        Integer estimatedSeconds,

        /** The measured rolling average once executions exist, otherwise {@code estimatedSeconds}. */
        Integer averageSeconds,

        int executionCount,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

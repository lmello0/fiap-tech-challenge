package com.fiap.techchallenge.inventory.api.representation;

import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PartInfo(
        UUID id,
        String sku,
        String name,
        String description,
        String brand,
        UnitOfMeasure unitOfMeasure,
        BigDecimal salePrice,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

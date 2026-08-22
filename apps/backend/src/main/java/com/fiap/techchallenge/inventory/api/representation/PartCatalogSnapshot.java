package com.fiap.techchallenge.inventory.api.representation;

import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A Part's Snapshot for History — catalog fields only. Deliberately excludes
 * {@code quantityOnHand}/{@code quantityReserved}/{@code available}: Stock Movement already owns
 * that history, and duplicating it here would give a reader two sources that can disagree (ADR 0012,
 * Q26).
 */
public record PartCatalogSnapshot(
        UUID id,
        String sku,
        String name,
        String description,
        String brand,
        UnitOfMeasure unitOfMeasure,
        BigDecimal salePrice,
        BigDecimal averageCost,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static PartCatalogSnapshot from(PartInfo info) {
        return new PartCatalogSnapshot(
                info.id(), info.sku(), info.name(), info.description(), info.brand(),
                info.unitOfMeasure(), info.salePrice(), info.averageCost(), info.active(),
                info.createdAt(), info.updatedAt());
    }
}

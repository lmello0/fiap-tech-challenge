package com.fiap.techchallenge.inventory.api.representation;

import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A Part's Snapshot for History — catalog fields only. Deliberately excludes on-hand/reserved/
 * available/cost: those are all derived from Stock Movement and Part Reservation, and duplicating
 * them here would give a reader two sources that can disagree (ADR 0012).
 */
public record PartCatalogSnapshot(
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

    public static PartCatalogSnapshot from(PartInfo info) {
        return new PartCatalogSnapshot(
                info.id(), info.sku(), info.name(), info.description(), info.brand(),
                info.unitOfMeasure(), info.salePrice(), info.active(),
                info.createdAt(), info.updatedAt());
    }
}

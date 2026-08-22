package com.fiap.techchallenge.inventory.api.representation;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * The Snapshot carried by {@code PartReservationExpiredEvent}. Deliberately doesn't reach into
 * {@code workorder} for the owning Work Order's own state — {@code inventory} depends on nothing
 * outside itself (ADR 0006's one-way dependency runs the other way).
 */
public record PartReservationSnapshot(
        UUID workOrderId,
        UUID partId,
        BigDecimal quantityReleased
) {
}

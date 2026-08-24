package com.fiap.techchallenge.inventory.api.events;

import java.util.List;
import java.util.UUID;

/**
 * A stock receipt or upward adjustment topped up one or more of a work order's shortfalled
 * reservations. Published on every partial heal, not only when the work order becomes fully
 * satisfiable — {@code remainingShortfallCount} is how a listener tells the difference. Not consumed
 * anywhere yet; published so a future listener (e.g. a customer notification) doesn't need inventory
 * to change to add one.
 */
public record WorkOrderPartsReplenishedEvent(
        UUID workOrderId,
        List<UUID> healedPartIds,
        int remainingShortfallCount
) {
}

package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.ReservePartCommand;
import com.fiap.techchallenge.inventory.api.representation.BlockingShortfallInfo;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Ties stock to a work order between quoting and starting service. Every reservation belongs to
 * exactly one work order — there is no way to reserve a part outside of one.
 */
public interface PartReservationService {

    /**
     * Best-effort: claims what's available for each request and records the rest as a shortfall.
     * Never throws for insufficient stock — only {@link #consumeForWorkOrder(UUID)} is strict. Safe to
     * call more than once for the same work order (e.g. requoting after diagnostics is redone); each
     * call adds its own reservations rather than replacing earlier ones.
     */
    void reserveForWorkOrder(UUID workOrderId, List<ReservePartCommand> requests);

    /**
     * Releases up to {@code quantity} of HELD reservations for {@code partId} on {@code workOrderId},
     * most-recently-reserved first, reducing or fully releasing reservation rows as needed. Used by a
     * budget draft line edit that reduces or removes a quantity — releases only the delta, never the
     * whole work order's reservations (see the Work Orders ADR on draft edits reserving/releasing live).
     */
    void releasePartial(UUID workOrderId, UUID partId, java.math.BigDecimal quantity);

    /**
     * Returns every {@code HELD} reservation's claimed stock to availability and marks them
     * {@code RELEASED}. A no-op if the work order holds no {@code HELD} reservations.
     */
    void releaseForWorkOrder(UUID workOrderId);

    /**
     * Writes off every {@code HELD} reservation's claimed stock and marks them {@code CONSUMED}.
     * Strict: throws {@link com.fiap.techchallenge.inventory.exceptions.InsufficientStockException} if
     * any reservation still carries a shortfall, and consumes nothing in that case.
     */
    void consumeForWorkOrder(UUID workOrderId);

    /**
     * Releases every {@code HELD} reservation whose {@code reservedAt} is before {@code cutoff} and
     * marks them {@code EXPIRED}, publishing a {@code PartReservationExpiredEvent} for each one that
     * actually held stock. Returns the number of reservations expired.
     */
    int expireStaleReservations(Instant cutoff);

    /**
     * The parts still short on {@code workOrderId}'s reservations — the answer to "is this work order
     * blocked from starting service because of stock?" Empty means nothing here is blocking it. Parts
     * only: budget, assignment, and status reasons are for the work-order module to compose alongside
     * this, not for inventory to know about.
     */
    List<BlockingShortfallInfo> getBlockingShortfalls(UUID workOrderId);
}

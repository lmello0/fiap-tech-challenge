package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.api.representation.PartReservationSnapshot;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A reservation aged past the reservation TTL with its work order never starting service. The
 * claimed stock has already been returned to availability by the time this is published. Belongs to
 * the work order's Timeline (CONTEXT.md "Timeline") — a Reservation always belongs to exactly one
 * work order.
 */
public record PartReservationExpiredEvent(
        UUID workOrderId,
        UUID partId,
        BigDecimal quantityReleased,
        EventMetadata metadata,
        PartReservationSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "PART_RESERVATION_EXPIRED";
    }

    @Override
    public String aggregateType() {
        return "WORK_ORDER";
    }

    @Override
    public UUID aggregateId() {
        return workOrderId;
    }

    @Override
    public String entityType() {
        return "PART_RESERVATION";
    }

    @Override
    public UUID entityId() {
        return partId;
    }
}

package com.fiap.techchallenge.scheduling.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

/**
 * Published by {@code workorder} when one of its Work Orders reaches WAITING_PICKUP, requesting the
 * pickup-booking invitation (CONTEXT.md "Pickup Appointment"). Owned by {@code scheduling} rather
 * than carrying {@code workorder}'s own {@code WorkOrderWaitingPickupEvent} across the boundary — the
 * dependency this creates points the same direction as the Check-in coordination (ADR 0014):
 * {@code workorder} depends on {@code scheduling.api.events}, never the reverse. Same shape as
 * {@code email.api.EmailRequestedEvent}, which every producing module depends on the same way.
 */
public record PickupInvitationRequestedEvent(
        UUID workOrderId,
        UUID customerId,
        UUID vehicleId,
        EventMetadata metadata
) implements DomainEvent {

    @Override
    public String eventType() {
        return "PICKUP_INVITATION_REQUESTED";
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
    public Object snapshot() {
        return this;
    }
}

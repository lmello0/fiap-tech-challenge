package com.fiap.techchallenge.scheduling.api.events;

import com.fiap.techchallenge.scheduling.api.representation.AppointmentSnapshot;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

/**
 * Published when Check-in completes an Appointment. The sole trigger for the {@code workorder}
 * side of the Appointment&harr;WorkOrder coordination (ADR 0014): a Drop-off check-in asks
 * {@code workorder} to create the WorkOrder, a Pickup check-in asks it to mark the WorkOrder
 * DELIVERED. {@code workorder} depends on this event type directly (not the generic
 * {@link DomainEvent} sink) since this is a targeted integration, not history recording.
 */
public record AppointmentCheckedInEvent(
        UUID appointmentId,
        AppointmentType appointmentType,
        UUID customerId,
        UUID vehicleId,
        UUID workOrderId,
        String complaint,
        EventMetadata metadata,
        AppointmentSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "APPOINTMENT_CHECKED_IN";
    }

    @Override
    public String aggregateType() {
        return AppointmentAggregate.TYPE;
    }

    @Override
    public UUID aggregateId() {
        return appointmentId;
    }
}

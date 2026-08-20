package com.fiap.techchallenge.scheduling.api.events;

import com.fiap.techchallenge.scheduling.api.representation.AppointmentSnapshot;
import com.fiap.techchallenge.scheduling.enums.AppointmentCancelReason;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

public record AppointmentCancelledEvent(
        UUID appointmentId,
        AppointmentCancelReason reason,
        EventMetadata metadata,
        AppointmentSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "APPOINTMENT_CANCELLED";
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

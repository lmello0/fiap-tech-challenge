package com.fiap.techchallenge.vehicle.api.events;

import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;

import java.util.UUID;

public record VehicleRegisteredEvent(
        UUID vehicleId,
        EventMetadata metadata,
        VehicleInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "VEHICLE_REGISTERED";
    }

    @Override
    public String aggregateType() {
        return VehicleAggregate.TYPE;
    }

    @Override
    public UUID aggregateId() {
        return vehicleId;
    }
}

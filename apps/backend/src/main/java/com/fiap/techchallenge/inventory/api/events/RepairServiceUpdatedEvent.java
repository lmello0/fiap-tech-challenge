package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

public record RepairServiceUpdatedEvent(
        UUID repairServiceId,
        EventMetadata metadata,
        RepairServiceInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "REPAIR_SERVICE_UPDATED";
    }

    @Override
    public String aggregateType() {
        return InventoryAggregates.REPAIR_SERVICE;
    }

    @Override
    public UUID aggregateId() {
        return repairServiceId;
    }
}

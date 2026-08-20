package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.api.representation.PartCatalogSnapshot;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

public record PartDeactivatedEvent(
        UUID partId,
        EventMetadata metadata,
        PartCatalogSnapshot snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "PART_DEACTIVATED";
    }

    @Override
    public String aggregateType() {
        return InventoryAggregates.PART;
    }

    @Override
    public UUID aggregateId() {
        return partId;
    }
}

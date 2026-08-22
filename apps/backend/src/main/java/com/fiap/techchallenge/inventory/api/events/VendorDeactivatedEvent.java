package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

public record VendorDeactivatedEvent(
        UUID vendorId,
        EventMetadata metadata,
        VendorInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "VENDOR_DEACTIVATED";
    }

    @Override
    public String aggregateType() {
        return InventoryAggregates.VENDOR;
    }

    @Override
    public UUID aggregateId() {
        return vendorId;
    }
}

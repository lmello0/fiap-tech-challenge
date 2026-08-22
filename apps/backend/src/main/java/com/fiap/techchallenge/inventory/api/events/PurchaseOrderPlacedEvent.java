package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

public record PurchaseOrderPlacedEvent(
        UUID purchaseOrderId,
        String code,
        UUID vendorId,
        EventMetadata metadata,
        PurchaseOrderInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "PURCHASE_ORDER_PLACED";
    }

    @Override
    public String aggregateType() {
        return InventoryAggregates.PURCHASE_ORDER;
    }

    @Override
    public UUID aggregateId() {
        return purchaseOrderId;
    }
}

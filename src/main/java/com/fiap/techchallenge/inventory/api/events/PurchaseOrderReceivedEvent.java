package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import com.fiap.techchallenge.shared.audit.DomainEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;

import java.util.UUID;

/**
 * Published after any receipt is recorded. {@code status} tells the listener whether this receipt
 * closed the purchase order ({@code RECEIVED}) or left it open ({@code PARTIALLY_RECEIVED}).
 */
public record PurchaseOrderReceivedEvent(
        UUID purchaseOrderId,
        String code,
        PurchaseOrderStatus status,
        EventMetadata metadata,
        PurchaseOrderInfo snapshot
) implements DomainEvent {

    @Override
    public String eventType() {
        return "PURCHASE_ORDER_RECEIVED";
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

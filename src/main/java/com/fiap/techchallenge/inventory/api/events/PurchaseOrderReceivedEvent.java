package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;

import java.util.UUID;

/**
 * Published after any receipt is recorded. {@code status} tells the listener whether this receipt
 * closed the purchase order ({@code RECEIVED}) or left it open ({@code PARTIALLY_RECEIVED}).
 */
public record PurchaseOrderReceivedEvent(
        UUID purchaseOrderId,
        String code,
        PurchaseOrderStatus status
) {
}

package com.fiap.techchallenge.inventory.api.events;

import java.util.UUID;

public record PurchaseOrderPlacedEvent(
        UUID purchaseOrderId,
        String code,
        UUID vendorId
) {
}

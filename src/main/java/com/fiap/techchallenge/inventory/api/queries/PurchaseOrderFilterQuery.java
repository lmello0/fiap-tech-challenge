package com.fiap.techchallenge.inventory.api.queries;

import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;

import java.util.UUID;

public record PurchaseOrderFilterQuery(
        UUID vendorId,
        PurchaseOrderStatus status,
        String code
) {
}

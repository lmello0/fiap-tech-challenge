package com.fiap.techchallenge.inventory.api.representation;

import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PurchaseOrderInfo(
        UUID id,
        String code,
        UUID vendorId,
        PurchaseOrderStatus status,
        String vendorOrderRef,
        Instant placedAt,
        Instant expectedAt,
        Instant receivedAt,
        Instant updatedAt,
        List<PurchaseOrderLineInfo> lines
) {
}

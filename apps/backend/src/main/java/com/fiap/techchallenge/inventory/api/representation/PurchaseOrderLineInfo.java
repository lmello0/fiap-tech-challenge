package com.fiap.techchallenge.inventory.api.representation;

import java.math.BigDecimal;
import java.util.UUID;

public record PurchaseOrderLineInfo(
        UUID id,
        UUID partId,
        BigDecimal quantityOrdered,
        BigDecimal quantityReceived,
        BigDecimal unitCost
) {
}

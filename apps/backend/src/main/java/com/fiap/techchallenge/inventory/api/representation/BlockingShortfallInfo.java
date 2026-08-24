package com.fiap.techchallenge.inventory.api.representation;

import java.math.BigDecimal;
import java.util.UUID;

/** One part still short on a work order's reservations — a reason it can't start service yet. */
public record BlockingShortfallInfo(
        UUID partId,
        String partSku,
        String partName,
        BigDecimal quantityShort
) {
}

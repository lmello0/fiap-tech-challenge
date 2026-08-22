package com.fiap.techchallenge.inventory.api.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A part's inventory position fell to or below its reorder rule's minimum, but the rule is disabled
 * — nobody gets auto-ordered stock, but somebody should probably know. Never published for a part
 * with no reorder rule at all; there's no minimum to have crossed.
 */
public record PartStockLowEvent(
        UUID partId,
        BigDecimal position,
        BigDecimal minQuantity
) {
}

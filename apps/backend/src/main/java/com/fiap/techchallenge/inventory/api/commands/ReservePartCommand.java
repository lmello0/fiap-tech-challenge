package com.fiap.techchallenge.inventory.api.commands;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * One line of a work order's request to reserve stock: this much of this part, for this quote.
 * Reserving is best-effort — the caller gets back what was actually claimed, never an exception, for
 * a request this command can't fully satisfy.
 */
public record ReservePartCommand(
        UUID partId,
        BigDecimal quantity
) {
}

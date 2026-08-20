package com.fiap.techchallenge.inventory.api.events;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A reservation aged past the reservation TTL with its work order never starting service. The
 * claimed stock has already been returned to availability by the time this is published.
 */
public record PartReservationExpiredEvent(
        UUID workOrderId,
        UUID partId,
        BigDecimal quantityReleased
) {
}

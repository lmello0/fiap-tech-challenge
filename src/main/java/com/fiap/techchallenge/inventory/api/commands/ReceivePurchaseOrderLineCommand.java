package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record ReceivePurchaseOrderLineCommand(
        @NotNull(message = "Line ID may not be null")
        UUID lineId,

        @NotNull(message = "Quantity received may not be null")
        @Positive(message = "Quantity received must be positive")
        BigDecimal quantityReceived,

        @NotNull(message = "Unit cost may not be null")
        @PositiveOrZero(message = "Unit cost must be positive or zero")
        BigDecimal unitCost
) {
}

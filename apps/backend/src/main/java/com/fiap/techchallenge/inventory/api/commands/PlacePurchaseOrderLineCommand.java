package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record PlacePurchaseOrderLineCommand(
        @NotNull(message = "Part ID may not be null")
        UUID partId,

        @NotNull(message = "Quantity may not be null")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity
) {
}

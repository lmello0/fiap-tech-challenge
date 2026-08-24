package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateStockPolicyCommand(
        @NotNull(message = "Minimum quantity may not be null")
        @PositiveOrZero(message = "Minimum quantity must be positive or zero")
        BigDecimal minQuantity,

        BigDecimal maxQuantity,

        UUID vendorId,

        @NotNull(message = "Auto-reorder enabled may not be null")
        Boolean autoReorderEnabled
) {
}

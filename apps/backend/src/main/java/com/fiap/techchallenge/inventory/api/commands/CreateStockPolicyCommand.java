package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * {@code maxQuantity} and {@code vendorId} are required only when {@code autoReorderEnabled} is
 * true — a policy that only flags low stock has no order-up-to target and no vendor to place with.
 */
public record CreateStockPolicyCommand(
        @NotNull(message = "Part ID may not be null")
        UUID partId,

        @NotNull(message = "Minimum quantity may not be null")
        @PositiveOrZero(message = "Minimum quantity must be positive or zero")
        BigDecimal minQuantity,

        BigDecimal maxQuantity,

        UUID vendorId,

        @NotNull(message = "Auto-reorder enabled may not be null")
        Boolean autoReorderEnabled
) {
}

package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateReorderRuleCommand(
        @NotNull(message = "Part ID may not be null")
        UUID partId,

        @NotNull(message = "Minimum quantity may not be null")
        @PositiveOrZero(message = "Minimum quantity must be positive or zero")
        BigDecimal minQuantity,

        @NotNull(message = "Maximum quantity may not be null")
        BigDecimal maxQuantity,

        @NotNull(message = "Vendor ID may not be null")
        UUID vendorId,

        @NotNull(message = "Enabled may not be null")
        Boolean enabled
) {
}

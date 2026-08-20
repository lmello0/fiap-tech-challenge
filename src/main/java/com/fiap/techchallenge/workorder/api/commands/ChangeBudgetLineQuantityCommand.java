package com.fiap.techchallenge.workorder.api.commands;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ChangeBudgetLineQuantityCommand(
        @NotNull(message = "Quantity may not be null")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity
) {
}

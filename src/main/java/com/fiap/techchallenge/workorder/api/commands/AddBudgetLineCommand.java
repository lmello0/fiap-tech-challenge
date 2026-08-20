package com.fiap.techchallenge.workorder.api.commands;

import com.fiap.techchallenge.workorder.enums.RowType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A line added to a Budget still in DRAFT. Description and unit price are never taken from the
 * caller — they are snapshotted from the inventory catalog at add-time. Exactly one of
 * {@code partId}/{@code serviceId} must be supplied, matching {@code type}.
 */
public record AddBudgetLineCommand(
        @NotNull(message = "Type may not be null")
        RowType type,

        @NotNull(message = "Quantity may not be null")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        UUID partId,

        UUID serviceId
) {
}

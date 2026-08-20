package com.fiap.techchallenge.workorder.api.commands;

import com.fiap.techchallenge.workorder.enums.RowType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A row on a work order's quote, identified by catalog reference rather than free text — description
 * and unit price are never taken from the caller. They are snapshotted from the inventory catalog at
 * quote time, so a later price change never rewrites what a customer already agreed to.
 *
 * <p>Exactly one of {@code partId}/{@code serviceId} must be supplied, matching {@code type}; the
 * service layer validates the pairing.
 */
public record CreateWorkOrderRowCommand(
        @NotNull(message = "Type may not be null")
        RowType type,

        @NotNull(message = "Quantity may not be null")
        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        UUID partId,

        UUID serviceId
) {
}

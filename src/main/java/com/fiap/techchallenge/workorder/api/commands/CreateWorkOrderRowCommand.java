package com.fiap.techchallenge.workorder.api.commands;

import com.fiap.techchallenge.workorder.enums.RowType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateWorkOrderRowCommand(
        @NotNull(message = "Type may not be null")
        RowType type,

        @NotBlank(message = "Description may not be blank")
        String description,

        @Positive(message = "Quantity must be positive")
        BigDecimal quantity,

        @PositiveOrZero(message = "Unit price must be positive or zero")
        BigDecimal unitPrice,

        UUID partId
) {
}

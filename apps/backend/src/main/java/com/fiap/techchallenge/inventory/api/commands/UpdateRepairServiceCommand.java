package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

public record UpdateRepairServiceCommand(
        @NotBlank(message = "Name may not be blank")
        @Length(max = 150, message = "Name may not exceed 150 characters")
        String name,

        @Length(max = 2000, message = "Description may not exceed 2_000 characters")
        String description,

        @NotNull(message = "Price may not be null")
        @PositiveOrZero(message = "Price must be positive or zero")
        BigDecimal price,

        @NotNull(message = "Estimated seconds may not be null")
        @Positive(message = "Estimated seconds must be positive")
        Integer estimatedSeconds
) {
}

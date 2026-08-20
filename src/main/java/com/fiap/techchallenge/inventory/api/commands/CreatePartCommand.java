package com.fiap.techchallenge.inventory.api.commands;

import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

public record CreatePartCommand(
        @NotBlank(message = "SKU may not be blank")
        @Length(max = 50, message = "SKU may not exceed 50 characters")
        String sku,

        @NotBlank(message = "Name may not be blank")
        @Length(max = 150, message = "Name may not exceed 150 characters")
        String name,

        @Length(max = 2000, message = "Description may not exceed 2_000 characters")
        String description,

        @Length(max = 100, message = "Brand may not exceed 100 characters")
        String brand,

        @NotNull(message = "Unit of measure may not be null")
        UnitOfMeasure unitOfMeasure,

        @NotNull(message = "Sale price may not be null")
        @PositiveOrZero(message = "Sale price must be positive or zero")
        BigDecimal salePrice
) {
}

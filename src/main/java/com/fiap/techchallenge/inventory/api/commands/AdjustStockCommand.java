package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.math.BigDecimal;

/**
 * A manual correction to a part's on-hand quantity — a physical count, breakage, or stock found on
 * the wrong shelf. {@code quantity} is signed: positive adds stock, negative removes it. Zero is
 * rejected by the service, since an adjustment that changes nothing has nothing to explain.
 */
public record AdjustStockCommand(
        @NotNull(message = "Quantity may not be null")
        BigDecimal quantity,

        @NotBlank(message = "Reason may not be blank")
        @Length(max = 500, message = "Reason may not exceed 500 characters")
        String reason
) {
}

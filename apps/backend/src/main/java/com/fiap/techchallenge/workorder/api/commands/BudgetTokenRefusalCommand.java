package com.fiap.techchallenge.workorder.api.commands;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record BudgetTokenRefusalCommand(
        @NotBlank(message = "Token may not be blank")
        String token,

        @Length(max = 2000, message = "Reason may not exceed 2_000 characters")
        String reason
) {
}

package com.fiap.techchallenge.workorder.api.commands;

import jakarta.validation.constraints.NotBlank;

public record BudgetTokenCommand(
        @NotBlank(message = "Token may not be blank")
        String token
) {
}

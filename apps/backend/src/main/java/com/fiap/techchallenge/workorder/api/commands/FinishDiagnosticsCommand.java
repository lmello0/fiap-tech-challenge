package com.fiap.techchallenge.workorder.api.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Length;

import java.util.List;

/**
 * Finishing diagnostics atomically drafts the work order's Budget, seeded with these lines
 * (ADR 0008/0009).
 */
public record FinishDiagnosticsCommand(
        @NotBlank(message = "Diagnosis may not be blank")
        @Length(max = 2000, message = "Diagnosis may not exceed 2_000 characters")
        String diagnosis,

        @Valid
        @NotEmpty
        List<AddBudgetLineCommand> lines
) {
}

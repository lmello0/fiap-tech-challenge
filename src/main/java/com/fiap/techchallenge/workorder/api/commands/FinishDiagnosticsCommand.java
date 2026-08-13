package com.fiap.techchallenge.workorder.api.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.Length;

import java.util.List;

public record FinishDiagnosticsCommand(
        @NotBlank(message = "Diagnosis may not be blank")
        @Length(max = 2000, message = "Diagnosis may not exceed 2_000 characters")
        String diagnosis,

        @Valid
        @NotEmpty
        List<CreateWorkOrderRowCommand> rows
) {
}

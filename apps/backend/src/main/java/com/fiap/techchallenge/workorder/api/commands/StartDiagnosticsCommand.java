package com.fiap.techchallenge.workorder.api.commands;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record StartDiagnosticsCommand(

        @NotNull(message = "Mechanic ID may not be null")
        UUID mechanicId
) {
}

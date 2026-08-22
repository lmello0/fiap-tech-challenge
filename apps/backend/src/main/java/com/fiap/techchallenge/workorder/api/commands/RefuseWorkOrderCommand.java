package com.fiap.techchallenge.workorder.api.commands;

import org.hibernate.validator.constraints.Length;

public record RefuseWorkOrderCommand(
        @Length(max = 2000, message = "Reason may not exceed 2_000 characters")
        String reason
) {
}

package com.fiap.techchallenge.user.api.commands;

import com.fiap.techchallenge.user.enums.WorkerRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateWorkerCommand(
        @Valid
        @NotNull(message = "User data may not be null")
        CreateUserCommand user,

        @NotNull(message = "Role may not be null")
        WorkerRole role,

        @NotNull(message = "Hire date may not be null")
        @PastOrPresent(message = "Hire date may not be in the future")
        LocalDate hireDate,

        @NotNull(message = "Start date may not be null")
        LocalDate startDate
) {
}

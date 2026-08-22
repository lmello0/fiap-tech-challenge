package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.time.LocalDate;

public record CreateClosureCommand(
        @NotNull(message = "Date may not be null")
        @Future(message = "Date must be in the future")
        LocalDate date,

        @Length(max = 500, message = "Message cannot exceed 500 characters")
        String message
) {
}

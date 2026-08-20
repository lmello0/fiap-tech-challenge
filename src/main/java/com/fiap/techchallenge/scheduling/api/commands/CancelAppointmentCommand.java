package com.fiap.techchallenge.scheduling.api.commands;

import org.hibernate.validator.constraints.Length;

public record CancelAppointmentCommand(
        @Length(max = 500, message = "Message cannot exceed 500 characters")
        String message
) {
}

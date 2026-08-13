package com.fiap.techchallenge.workorder.api.commands;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

import java.util.UUID;

public record CreateWorkOrderCommand(

        @NotNull(message = "Customer ID may not be null")
        UUID customerId,

        @NotNull(message = "Vehicle ID may not be null")
        UUID vehicleId,

        @NotBlank(message = "Complaint may not be blank")
        @Length(max = 2000, message = "Complaint cannot exceed 2_000 characters")
        String complaint
) {
}

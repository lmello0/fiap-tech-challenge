package com.fiap.techchallenge.scheduling.api.commands;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.Length;

import java.time.Instant;

public record BookGuestDropoffCommand(
        @NotBlank(message = "Name may not be blank")
        @Size(max = 100, message = "Name may not exceed 100 characters")
        String guestName,

        @NotBlank(message = "Phone may not be blank")
        @Size(max = 30, message = "Phone may not exceed 30 characters")
        String guestPhone,

        @NotBlank(message = "Email may not be blank")
        @Email
        @Size(max = 255, message = "Email may not exceed 255 characters")
        String guestEmail,

        @NotBlank(message = "Vehicle make may not be blank")
        @Size(max = 50, message = "Vehicle make may not exceed 50 characters")
        String guestVehicleMake,

        @NotBlank(message = "Vehicle model may not be blank")
        @Size(max = 100, message = "Vehicle model may not exceed 100 characters")
        String guestVehicleModel,

        @NotNull(message = "Vehicle year may not be null")
        Integer guestVehicleYear,

        @NotBlank(message = "Complaint may not be blank")
        @Length(max = 2000, message = "Complaint cannot exceed 2_000 characters")
        String complaint,

        @NotNull(message = "Slot start may not be null")
        @Future(message = "Slot start must be in the future")
        Instant slotStart
) {
}

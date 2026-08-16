package com.fiap.techchallenge.user.api.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateUserProfileCommand(
        @NotBlank(message = "First name may not be blank")
        @Size(min = 3, max = 30, message = "First name must be between 3 and 30 characters")
        String firstName,

        @Size(max = 30, message = "Last name may not exceed 30 characters")
        String lastName,

        @NotEmpty(message = "At least 1 phone number is required")
        List<@Valid RegisterPhoneNumberCommand> phoneNumbers
) {
}

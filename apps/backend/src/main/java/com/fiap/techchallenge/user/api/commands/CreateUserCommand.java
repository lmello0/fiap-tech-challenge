package com.fiap.techchallenge.user.api.commands;

import com.fiap.techchallenge.user.enums.DocumentType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

public record CreateUserCommand(
        @Email
        @NotBlank(message = "Email may not be blank")
        @Size(max = 255, message = "Email may not exceed 255 characters")
        String email,

        @NotBlank(message = "First name may not be blank")
        @Size(min = 3, max = 30, message = "First name must be between 3 and 30 characters")
        String firstName,

        @Size(max = 30, message = "Last name may not exceed 30 characters")
        String lastName,

        @NotNull(message = "Document type may not be null")
        DocumentType documentType,

        @NotBlank(message = "Document code may not be blank")
        @Size(max = 50, message = "Document code may not exceed 50 characters")
        String documentCode,

        @NotEmpty(message = "At least 1 phone number is required")
        List<@Valid RegisterPhoneNumberCommand> phoneNumbers
) {
}

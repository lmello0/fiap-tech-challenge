package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record UpdateVendorCommand(
        @NotBlank(message = "Name may not be blank")
        @Length(max = 150, message = "Name may not exceed 150 characters")
        String name,

        @Email(message = "Contact email must be a valid email address")
        @Length(max = 255, message = "Contact email may not exceed 255 characters")
        String contactEmail
) {
}

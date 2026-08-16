package com.fiap.techchallenge.user.api.commands;

import com.fiap.techchallenge.user.enums.PhoneType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterPhoneNumberCommand(
        @NotNull(message = "Type may not be null")
        PhoneType type,

        @NotBlank(message = "Phone may not be blank")
        String phone,

        boolean isPrimary
) {
}

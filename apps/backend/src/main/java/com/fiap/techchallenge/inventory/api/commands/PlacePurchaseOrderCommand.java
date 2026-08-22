package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record PlacePurchaseOrderCommand(
        @NotNull(message = "Vendor ID may not be null")
        UUID vendorId,

        @Valid
        @NotEmpty(message = "A purchase order needs at least one line")
        List<PlacePurchaseOrderLineCommand> lines
) {
}

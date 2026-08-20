package com.fiap.techchallenge.inventory.api.commands;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReceivePurchaseOrderCommand(
        @Valid
        @NotEmpty(message = "A receipt needs at least one line")
        List<ReceivePurchaseOrderLineCommand> lines
) {
}

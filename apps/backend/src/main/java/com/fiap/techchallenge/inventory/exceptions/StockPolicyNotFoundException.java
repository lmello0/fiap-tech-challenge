package com.fiap.techchallenge.inventory.exceptions;

import java.util.UUID;

public class StockPolicyNotFoundException extends RuntimeException {
    public StockPolicyNotFoundException(UUID id) {
        super("Reorder rule not found: " + id);
    }
}

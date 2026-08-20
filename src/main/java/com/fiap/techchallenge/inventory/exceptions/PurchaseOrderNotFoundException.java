package com.fiap.techchallenge.inventory.exceptions;

import java.util.UUID;

public class PurchaseOrderNotFoundException extends RuntimeException {
    public PurchaseOrderNotFoundException(UUID id) {
        super("Purchase order not found: " + id);
    }
}

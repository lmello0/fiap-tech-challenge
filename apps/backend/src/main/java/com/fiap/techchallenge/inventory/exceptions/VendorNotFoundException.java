package com.fiap.techchallenge.inventory.exceptions;

import java.util.UUID;

public class VendorNotFoundException extends RuntimeException {
    public VendorNotFoundException(UUID id) {
        super("Vendor not found: " + id);
    }
}

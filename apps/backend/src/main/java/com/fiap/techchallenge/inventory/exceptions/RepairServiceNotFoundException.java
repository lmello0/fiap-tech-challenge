package com.fiap.techchallenge.inventory.exceptions;

import java.util.UUID;

public class RepairServiceNotFoundException extends RuntimeException {
    public RepairServiceNotFoundException(UUID id) {
        super("Service not found: " + id);
    }
}

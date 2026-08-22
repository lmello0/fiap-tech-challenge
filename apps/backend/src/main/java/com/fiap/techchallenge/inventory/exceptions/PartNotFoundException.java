package com.fiap.techchallenge.inventory.exceptions;

import java.util.UUID;

public class PartNotFoundException extends RuntimeException {
    public PartNotFoundException(UUID id) {
        super("Part not found: " + id);
    }
}

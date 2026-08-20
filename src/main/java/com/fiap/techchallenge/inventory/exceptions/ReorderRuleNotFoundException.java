package com.fiap.techchallenge.inventory.exceptions;

import java.util.UUID;

public class ReorderRuleNotFoundException extends RuntimeException {
    public ReorderRuleNotFoundException(UUID id) {
        super("Reorder rule not found: " + id);
    }
}

package com.fiap.techchallenge.workorder.exceptions;

import java.util.UUID;

public class BudgetLineNotFoundException extends RuntimeException {
    public BudgetLineNotFoundException(UUID id) {
        super("Budget line not found: " + id);
    }
}

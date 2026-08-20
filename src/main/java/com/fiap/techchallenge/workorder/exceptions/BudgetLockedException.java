package com.fiap.techchallenge.workorder.exceptions;

import java.util.UUID;

/** Thrown when a caller tries to edit a Budget's lines once it has left DRAFT (ADR 0010). */
public class BudgetLockedException extends RuntimeException {
    public BudgetLockedException(UUID budgetId) {
        super("Budget " + budgetId + " is locked and can no longer be edited");
    }
}

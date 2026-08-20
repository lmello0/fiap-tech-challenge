package com.fiap.techchallenge.workorder.exceptions;

import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import lombok.Getter;

@Getter
public class IllegalBudgetTransitionException extends RuntimeException {

    private final BudgetStatus from;
    private final BudgetStatus to;

    public IllegalBudgetTransitionException(BudgetStatus from, BudgetStatus to) {
        super("Illegal budget transition: " + from + " -> " + to);

        this.from = from;
        this.to = to;
    }
}

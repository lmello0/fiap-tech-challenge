package com.fiap.techchallenge.workorder.exceptions;

public class InvalidBudgetTokenException extends RuntimeException {

    public InvalidBudgetTokenException() {
        super("Invalid budget decision token");
    }
}

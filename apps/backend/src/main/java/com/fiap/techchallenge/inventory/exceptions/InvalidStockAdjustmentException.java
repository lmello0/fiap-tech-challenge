package com.fiap.techchallenge.inventory.exceptions;

public class InvalidStockAdjustmentException extends RuntimeException {
    public InvalidStockAdjustmentException(String message) {
        super(message);
    }
}

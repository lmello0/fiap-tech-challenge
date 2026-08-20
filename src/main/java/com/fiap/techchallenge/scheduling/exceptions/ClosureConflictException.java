package com.fiap.techchallenge.scheduling.exceptions;

import java.time.LocalDate;

public class ClosureConflictException extends RuntimeException {

    public ClosureConflictException(LocalDate date) {
        super("A closure already exists for " + date);
    }
}

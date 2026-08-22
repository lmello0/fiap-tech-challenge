package com.fiap.techchallenge.user.exceptions;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public class InvalidWorkerStartDateException extends RuntimeException {
    public InvalidWorkerStartDateException(
            LocalDate startDate,
            LocalDate hireDate) {
        super("Invalid worker start date, it must be equal or after hire date: " + startDate + " < " + hireDate);
    }
}

package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.shared.exceptions.ExceptionHandlerOrder;
import com.fiap.techchallenge.shared.exceptions.ProblemDetails;
import com.fiap.techchallenge.workorder.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(ExceptionHandlerOrder.MODULE)
public class WorkOrderExceptionHandler {

    @ExceptionHandler(WorkOrderNotFoundException.class)
    ProblemDetail handleNotFound(WorkOrderNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(BudgetNotFoundException.class)
    ProblemDetail handleBudgetNotFound(BudgetNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(BudgetLineNotFoundException.class)
    ProblemDetail handleLineNotFound(BudgetLineNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(IllegalWorkOrderTransitionException.class)
    ProblemDetail handleIllegalTransition(IllegalWorkOrderTransitionException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Illegal status transition", e.getMessage());
    }

    @ExceptionHandler(IllegalBudgetTransitionException.class)
    ProblemDetail handleIllegalBudgetTransition(IllegalBudgetTransitionException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Illegal budget transition", e.getMessage());
    }

    @ExceptionHandler(BudgetLockedException.class)
    ProblemDetail handleBudgetLocked(BudgetLockedException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Budget locked", e.getMessage());
    }

    @ExceptionHandler(WorkOrderNotInProgressException.class)
    ProblemDetail handleNotInProgress(WorkOrderNotInProgressException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Work order not in progress", e.getMessage());
    }

    @ExceptionHandler(VehicleOwnershipException.class)
    ProblemDetail handleVehicleOwnership(VehicleOwnershipException e) {
        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "Vehicle does not belong to customer", e.getMessage());
    }
}

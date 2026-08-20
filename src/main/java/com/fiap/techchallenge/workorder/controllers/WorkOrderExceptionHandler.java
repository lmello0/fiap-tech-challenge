package com.fiap.techchallenge.workorder.controllers;

import com.fiap.techchallenge.shared.exceptions.ExceptionHandlerOrder;
import com.fiap.techchallenge.shared.exceptions.ProblemDetails;
import com.fiap.techchallenge.workorder.exceptions.IllegalWorkOrderTransitionException;
import com.fiap.techchallenge.workorder.exceptions.WorkOrderNotFoundException;
import com.fiap.techchallenge.workorder.exceptions.WorkOrderNotInProgressException;
import com.fiap.techchallenge.workorder.exceptions.WorkOrderRowNotFoundException;
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

    @ExceptionHandler(WorkOrderRowNotFoundException.class)
    ProblemDetail handleRowNotFound(WorkOrderRowNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler(IllegalWorkOrderTransitionException.class)
    ProblemDetail handleIllegalTransition(IllegalWorkOrderTransitionException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Illegal status transition", e.getMessage());
    }

    @ExceptionHandler(WorkOrderNotInProgressException.class)
    ProblemDetail handleNotInProgress(WorkOrderNotInProgressException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Work order not in progress", e.getMessage());
    }
}

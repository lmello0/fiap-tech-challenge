package com.fiap.techchallenge.vehicle.controllers;

import com.fiap.techchallenge.shared.exceptions.ExceptionHandlerOrder;
import com.fiap.techchallenge.shared.exceptions.ProblemDetails;
import com.fiap.techchallenge.vehicle.exceptions.VehicleNotFoundException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(ExceptionHandlerOrder.MODULE)
public class VehicleExceptionHandler {

    @ExceptionHandler(VehicleNotFoundException.class)
    ProblemDetail handleNotFound(VehicleNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }
}

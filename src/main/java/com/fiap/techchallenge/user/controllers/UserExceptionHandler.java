package com.fiap.techchallenge.user.controllers;

import com.fiap.techchallenge.shared.exceptions.ExceptionHandlerOrder;
import com.fiap.techchallenge.shared.exceptions.ProblemDetails;
import com.fiap.techchallenge.user.exceptions.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@Order(ExceptionHandlerOrder.MODULE)
public class UserExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handleNotFound(UserNotFoundException e) {
        return ProblemDetails.of(HttpStatus.NOT_FOUND, "Not found", e.getMessage());
    }

    @ExceptionHandler({
            EmailAlreadyInUseException.class,
            DocumentAlreadyInUseException.class,
            RegistrationAlreadyInUseException.class,
            MultiplePrimaryPhoneNumberException.class
    })
    ProblemDetail handleConflict(RuntimeException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Conflict", e.getMessage());
    }

    @ExceptionHandler(NotAWorkerException.class)
    ProblemDetail handleNotAWorker(NotAWorkerException e) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_CONTENT, "Unprocessable", e.getMessage());
    }

    @ExceptionHandler(NotACustomerException.class)
    ProblemDetail handleNotACustomer(NotACustomerException e) {
        return ProblemDetails.of(HttpStatus.UNPROCESSABLE_CONTENT, "Unprocessable", e.getMessage());
    }
}

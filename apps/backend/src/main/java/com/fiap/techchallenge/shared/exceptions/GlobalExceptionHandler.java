package com.fiap.techchallenge.shared.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles only what every module can raise: framework and JDK types. Module-specific exceptions are
 * mapped by that module's own advice, which runs at {@link ExceptionHandlerOrder#MODULE} and therefore
 * always wins over the {@link Exception} catch-all below.
 */
@Slf4j
@RestControllerAdvice
@Order(ExceptionHandlerOrder.GLOBAL)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException e) {
        log.debug("Constraint violation: {}", e.getMessage());

        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "Validation failed", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleIllegalArgument(IllegalArgumentException e) {
        log.debug("Illegal argument: {}", e.getMessage());

        return ProblemDetails.of(HttpStatus.BAD_REQUEST, "Invalid request", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail handleAccessDenied(AccessDeniedException e) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, "Access denied", "You may not access this resource");
    }

    /**
     * Any module's repository can trip a unique index. The message carries SQL and column names, so it
     * is logged rather than returned.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("Data integrity violation", e);

        return ProblemDetails.of(HttpStatus.CONFLICT, "Conflict", "The request conflicts with existing data");
    }

    /**
     * Last resort. The detail is deliberately generic: an unmapped failure must never leak an exception
     * message or a stack frame to the caller.
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);

        return ProblemDetails.of(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                "Something went wrong while processing the request"
        );
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();

        for (FieldError error : e.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail body = ProblemDetails.of(
                HttpStatus.BAD_REQUEST, "Validation failed", "Request contains invalid fields");
        body.setProperty("errors", errors);

        return ResponseEntity.badRequest().body(body);
    }
}

package com.fiap.techchallenge.auth.controllers;

import com.fiap.techchallenge.auth.exceptions.AccountDisabledException;
import com.fiap.techchallenge.auth.exceptions.AccountLockedException;
import com.fiap.techchallenge.auth.exceptions.EmailNotVerifiedException;
import com.fiap.techchallenge.auth.exceptions.InvalidTokenException;
import com.fiap.techchallenge.auth.exceptions.RegistrationConflictException;
import com.fiap.techchallenge.shared.exceptions.ExceptionHandlerOrder;
import com.fiap.techchallenge.shared.exceptions.ProblemDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@Slf4j
@RestControllerAdvice
@Order(ExceptionHandlerOrder.MODULE)
public class AuthExceptionHandler {

    @ExceptionHandler({
            BadCredentialsException.class,
            InvalidTokenException.class
    })
    ProblemDetail handleUnauthorized(RuntimeException e) {
        log.debug("Authentication failed: {}", e.getMessage());

        return ProblemDetails.of(HttpStatus.UNAUTHORIZED, "Authentication failed", "Invalid credentials");
    }

    @ExceptionHandler(AccountDisabledException.class)
    ProblemDetail handleDisabled(AccountDisabledException e) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, "Account disabled", e.getMessage());
    }

    @ExceptionHandler(AccountLockedException.class)
    ProblemDetail handleLocked(AccountLockedException e) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, "Account locked", e.getMessage());
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    ProblemDetail handleEmailNotVerified(EmailNotVerifiedException e) {
        return ProblemDetails.of(HttpStatus.FORBIDDEN, "Email not verified", e.getMessage());
    }

    @ExceptionHandler(RegistrationConflictException.class)
    ProblemDetail handleRegistrationConflict(RegistrationConflictException e) {
        return ProblemDetails.of(HttpStatus.CONFLICT, "Conflict", e.getMessage());
    }
}

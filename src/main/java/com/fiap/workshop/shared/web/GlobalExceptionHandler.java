package com.fiap.workshop.shared.web;

import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail notFound(NotFoundException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail conflict(ConflictException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail integrity(DataIntegrityViolationException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The resource conflicts with an existing one");
	}

	@ExceptionHandler(DomainException.class)
	ProblemDetail domain(DomainException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, e.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	ProblemDetail denied(AccessDeniedException e) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied");
	}
}

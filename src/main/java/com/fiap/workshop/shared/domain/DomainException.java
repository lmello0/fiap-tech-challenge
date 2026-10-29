package com.fiap.workshop.shared.domain;

/** Base type for business-rule violations raised by the domain layer. */
public class DomainException extends RuntimeException {

	public DomainException(String message) {
		super(message);
	}
}

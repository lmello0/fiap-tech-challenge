package com.fiap.workshop.workorder.domain;

import com.fiap.workshop.shared.domain.ConflictException;

public class WorkOrderStateException extends ConflictException {

	public WorkOrderStateException(String message) {
		super(message);
	}
}

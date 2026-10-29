package com.fiap.workshop.customer.adapter.in.web;

import com.fiap.workshop.customer.domain.Customer;
import java.time.Instant;
import java.util.UUID;

record CustomerResponse(UUID id, String name, String document, String documentType, String email, String phone,
		boolean active, Instant createdAt, Instant updatedAt) {

	static CustomerResponse from(Customer c) {
		return new CustomerResponse(c.id(), c.name(), c.document().value(), c.document().type().name(), c.email().value(),
				c.phone(), c.active(), c.createdAt(), c.updatedAt());
	}
}

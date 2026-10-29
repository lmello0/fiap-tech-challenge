package com.fiap.workshop.customer.application.port.in;

import com.fiap.workshop.customer.domain.Customer;

public interface RegisterCustomerUseCase {

	Customer execute(Command command);

	record Command(String name, String document, String email, String phone) {
	}
}

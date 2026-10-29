package com.fiap.workshop.customer.application.port.in;

import com.fiap.workshop.customer.domain.Customer;
import java.util.UUID;

public interface UpdateCustomerUseCase {

	Customer execute(Command command);

	record Command(UUID id, String name, String email, String phone) {
	}
}

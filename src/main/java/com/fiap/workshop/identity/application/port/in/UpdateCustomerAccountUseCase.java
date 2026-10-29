package com.fiap.workshop.identity.application.port.in;

import java.util.UUID;

public interface UpdateCustomerAccountUseCase {

	void execute(Command command);

	record Command(UUID customerId, String name, String email) {
	}
}

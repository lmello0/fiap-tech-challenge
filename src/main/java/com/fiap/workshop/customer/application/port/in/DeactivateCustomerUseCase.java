package com.fiap.workshop.customer.application.port.in;

import java.util.UUID;

public interface DeactivateCustomerUseCase {

	void execute(UUID id);
}

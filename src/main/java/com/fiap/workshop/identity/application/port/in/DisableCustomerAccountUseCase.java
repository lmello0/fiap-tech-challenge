package com.fiap.workshop.identity.application.port.in;

import java.util.UUID;

public interface DisableCustomerAccountUseCase {

	void execute(UUID customerId);
}

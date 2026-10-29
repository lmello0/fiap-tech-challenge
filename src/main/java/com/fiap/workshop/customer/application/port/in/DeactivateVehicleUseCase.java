package com.fiap.workshop.customer.application.port.in;

import java.util.UUID;

public interface DeactivateVehicleUseCase {

	void execute(UUID id);
}

package com.fiap.workshop.customer.application.port.in;

import com.fiap.workshop.customer.domain.Vehicle;
import java.util.UUID;

public interface UpdateVehicleUseCase {

	Vehicle execute(Command command);

	record Command(UUID id, String brand, String model, int year) {
	}
}

package com.fiap.workshop.customer.application.port.in;

import com.fiap.workshop.customer.domain.Vehicle;
import java.util.UUID;

public interface RegisterVehicleUseCase {

	Vehicle execute(Command command);

	record Command(UUID customerId, String plate, String brand, String model, int year) {
	}
}

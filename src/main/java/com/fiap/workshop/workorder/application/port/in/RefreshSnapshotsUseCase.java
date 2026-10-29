package com.fiap.workshop.workorder.application.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public interface RefreshSnapshotsUseCase {

	void refreshCustomer(UUID customerId, String name, String document, String email);

	void refreshVehicle(UUID vehicleId, String plate, String brand, String model, int year);

	void refreshService(UUID serviceId, String name, BigDecimal price);

	void refreshPart(UUID partId, String name, BigDecimal price);
}

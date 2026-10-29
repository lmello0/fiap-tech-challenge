package com.fiap.workshop.workorder.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface CustomerLookup {

	Optional<CustomerInfo> findCustomerByDocument(String document);

	Optional<VehicleInfo> findVehicleByPlate(String plate);

	record CustomerInfo(UUID id, String name, String document, String email, boolean active) {
	}

	record VehicleInfo(UUID id, UUID customerId, String plate, String brand, String model, int year, boolean active) {
	}
}

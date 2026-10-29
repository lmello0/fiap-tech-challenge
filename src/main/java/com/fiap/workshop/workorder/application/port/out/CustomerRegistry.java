package com.fiap.workshop.workorder.application.port.out;

import java.util.UUID;

public interface CustomerRegistry {

	CustomerInfo ensureCustomer(CustomerDetails details);

	VehicleInfo ensureVehicle(UUID customerId, VehicleDetails details);

	record CustomerDetails(String document, String name, String email, String phone) {
	}

	record VehicleDetails(String plate, String brand, String model, int year) {
	}

	record CustomerInfo(UUID id, String name, String document, String email) {
	}

	record VehicleInfo(UUID id, UUID customerId, String plate, String brand, String model, int year) {
	}
}

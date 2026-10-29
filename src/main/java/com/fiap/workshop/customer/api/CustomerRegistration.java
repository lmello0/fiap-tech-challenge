package com.fiap.workshop.customer.api;

import java.util.UUID;

public interface CustomerRegistration {

	CustomerView ensureCustomer(CustomerData data);

	VehicleView ensureVehicle(UUID customerId, VehicleData data);

	record CustomerData(String document, String name, String email, String phone) {
	}

	record VehicleData(String plate, String brand, String model, int year) {
	}
}

package com.fiap.workshop.workorder.adapter.out.lookup;

import com.fiap.workshop.customer.api.CustomerRegistration;
import com.fiap.workshop.workorder.application.port.out.CustomerRegistry;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CustomerRegistryAdapter implements CustomerRegistry {

	private final CustomerRegistration registration;

	CustomerRegistryAdapter(CustomerRegistration registration) {
		this.registration = registration;
	}

	@Override
	public CustomerInfo ensureCustomer(CustomerDetails details) {
		var customer = registration.ensureCustomer(
				new CustomerRegistration.CustomerData(details.document(), details.name(), details.email(), details.phone()));
		return new CustomerInfo(customer.id(), customer.name(), customer.document(), customer.email());
	}

	@Override
	public VehicleInfo ensureVehicle(UUID customerId, VehicleDetails details) {
		var vehicle = registration.ensureVehicle(customerId,
				new CustomerRegistration.VehicleData(details.plate(), details.brand(), details.model(), details.year()));
		return new VehicleInfo(vehicle.id(), vehicle.customerId(), vehicle.plate(), vehicle.brand(), vehicle.model(), vehicle.year());
	}
}

package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.api.CustomerRegistration;
import com.fiap.workshop.customer.api.CustomerView;
import com.fiap.workshop.customer.api.VehicleView;
import com.fiap.workshop.customer.application.port.in.RegisterCustomerUseCase;
import com.fiap.workshop.customer.application.port.in.RegisterVehicleUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.domain.Document;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.LicensePlate;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CustomerRegistrationService implements CustomerRegistration {

	private final CustomerRepository customers;
	private final VehicleRepository vehicles;
	private final RegisterCustomerUseCase registerCustomer;
	private final RegisterVehicleUseCase registerVehicle;

	CustomerRegistrationService(CustomerRepository customers, VehicleRepository vehicles, RegisterCustomerUseCase registerCustomer,
			RegisterVehicleUseCase registerVehicle) {
		this.customers = customers;
		this.vehicles = vehicles;
		this.registerCustomer = registerCustomer;
		this.registerVehicle = registerVehicle;
	}

	@Override
	@Transactional
	public CustomerView ensureCustomer(CustomerData data) {
		Customer customer = customers.findByDocument(Document.of(data.document()))
				.orElseGet(() -> registerCustomer.execute(
						new RegisterCustomerUseCase.Command(data.name(), data.document(), data.email(), data.phone())));
		customer.requireActive();
		return CustomerQueriesService.view(customer);
	}

	@Override
	@Transactional
	public VehicleView ensureVehicle(UUID customerId, VehicleData data) {
		Vehicle vehicle = vehicles.findByPlate(LicensePlate.of(data.plate()))
				.orElseGet(() -> registerVehicle.execute(
						new RegisterVehicleUseCase.Command(customerId, data.plate(), data.brand(), data.model(), data.year())));
		if (!vehicle.customerId().equals(customerId)) {
			throw new DomainException("The vehicle does not belong to this customer");
		}
		vehicle.requireActive();
		return CustomerQueriesService.view(vehicle);
	}
}

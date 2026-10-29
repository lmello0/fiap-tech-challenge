package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.api.CustomerQueries;
import com.fiap.workshop.customer.api.CustomerView;
import com.fiap.workshop.customer.api.VehicleView;
import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.domain.Document;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.LicensePlate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CustomerQueriesService implements CustomerQueries {

	private final CustomerRepository customers;
	private final VehicleRepository vehicles;

	CustomerQueriesService(CustomerRepository customers, VehicleRepository vehicles) {
		this.customers = customers;
		this.vehicles = vehicles;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CustomerView> findCustomerById(UUID id) {
		return customers.findById(id).map(CustomerQueriesService::view);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CustomerView> findCustomerByDocument(String document) {
		return parse(() -> customers.findByDocument(Document.of(document))).map(CustomerQueriesService::view);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<VehicleView> findVehicleById(UUID id) {
		return vehicles.findById(id).map(CustomerQueriesService::view);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<VehicleView> findVehicleByPlate(String plate) {
		return parse(() -> vehicles.findByPlate(LicensePlate.of(plate))).map(CustomerQueriesService::view);
	}

	private static <T> Optional<T> parse(java.util.function.Supplier<Optional<T>> lookup) {
		try {
			return lookup.get();
		} catch (DomainException invalidInput) {
			return Optional.empty();
		}
	}

	private static CustomerView view(Customer c) {
		return new CustomerView(c.id(), c.name(), c.document().value(), c.email().value(), c.phone(), c.active());
	}

	private static VehicleView view(Vehicle v) {
		return new VehicleView(v.id(), v.customerId(), v.plate().value(), v.brand(), v.model(), v.year(), v.active());
	}
}

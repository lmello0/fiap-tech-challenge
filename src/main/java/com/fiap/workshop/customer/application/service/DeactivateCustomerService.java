package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.DeactivateCustomerUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.application.port.out.VehicleRepository.VehicleFilter;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeactivateCustomerService implements DeactivateCustomerUseCase {

	private static final int BATCH = 200;

	private final CustomerRepository customers;
	private final VehicleRepository vehicles;
	private final EventPublisher events;
	private final Clock clock;

	DeactivateCustomerService(CustomerRepository customers, VehicleRepository vehicles, EventPublisher events, Clock clock) {
		this.customers = customers;
		this.vehicles = vehicles;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void execute(UUID id) {
		Customer customer = customers.findById(id).orElseThrow(() -> new NotFoundException("Customer " + id + " not found"));
		customer.deactivate(clock.instant());
		customers.save(customer);
		List<Vehicle> batch;
		while (!(batch = vehicles.search(new VehicleFilter(id, false), new PageQuery(0, BATCH)).content()).isEmpty()) {
			batch.forEach(vehicle -> {
				vehicle.deactivate(clock.instant());
				vehicles.save(vehicle);
				events.publish(CustomerEvents.deactivated(vehicle, clock.instant()));
			});
		}
		events.publish(CustomerEvents.deactivated(customer, clock.instant()));
	}
}

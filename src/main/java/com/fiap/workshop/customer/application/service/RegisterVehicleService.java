package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.RegisterVehicleUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.LicensePlate;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterVehicleService implements RegisterVehicleUseCase {

	private final CustomerRepository customers;
	private final VehicleRepository vehicles;
	private final EventPublisher events;
	private final Clock clock;

	RegisterVehicleService(CustomerRepository customers, VehicleRepository vehicles, EventPublisher events, Clock clock) {
		this.customers = customers;
		this.vehicles = vehicles;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Vehicle execute(Command command) {
		customers.findById(command.customerId())
				.orElseThrow(() -> new NotFoundException("Customer " + command.customerId() + " not found"))
				.requireActive();
		LicensePlate plate = LicensePlate.of(command.plate());
		if (vehicles.existsByPlate(plate)) {
			throw new ConflictException("A vehicle with this plate already exists");
		}
		Vehicle vehicle = vehicles.save(
				Vehicle.register(command.customerId(), plate, command.brand(), command.model(), command.year(), clock));
		events.publish(CustomerEvents.registered(vehicle, clock.instant()));
		return vehicle;
	}
}

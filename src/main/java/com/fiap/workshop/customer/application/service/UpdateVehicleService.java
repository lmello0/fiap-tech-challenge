package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.UpdateVehicleUseCase;
import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateVehicleService implements UpdateVehicleUseCase {

	private final VehicleRepository vehicles;
	private final EventPublisher events;
	private final Clock clock;

	UpdateVehicleService(VehicleRepository vehicles, EventPublisher events, Clock clock) {
		this.vehicles = vehicles;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Vehicle execute(Command command) {
		Vehicle vehicle = vehicles.findById(command.id())
				.orElseThrow(() -> new NotFoundException("Vehicle " + command.id() + " not found"));
		vehicle.update(command.brand(), command.model(), command.year(), clock);
		vehicles.save(vehicle);
		events.publish(CustomerEvents.updated(vehicle, clock.instant()));
		return vehicle;
	}
}

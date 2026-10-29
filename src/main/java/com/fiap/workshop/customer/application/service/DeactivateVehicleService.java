package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.DeactivateVehicleUseCase;
import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeactivateVehicleService implements DeactivateVehicleUseCase {

	private final VehicleRepository vehicles;
	private final EventPublisher events;
	private final Clock clock;

	DeactivateVehicleService(VehicleRepository vehicles, EventPublisher events, Clock clock) {
		this.vehicles = vehicles;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void execute(UUID id) {
		Vehicle vehicle = vehicles.findById(id).orElseThrow(() -> new NotFoundException("Vehicle " + id + " not found"));
		vehicle.deactivate(clock.instant());
		vehicles.save(vehicle);
		events.publish(CustomerEvents.deactivated(vehicle, clock.instant()));
	}
}

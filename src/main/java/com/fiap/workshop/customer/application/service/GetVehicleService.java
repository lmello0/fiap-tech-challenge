package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.GetVehicleUseCase;
import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetVehicleService implements GetVehicleUseCase {

	private final VehicleRepository vehicles;

	GetVehicleService(VehicleRepository vehicles) {
		this.vehicles = vehicles;
	}

	@Override
	@Transactional(readOnly = true)
	public Vehicle execute(UUID id) {
		return vehicles.findById(id).orElseThrow(() -> new NotFoundException("Vehicle " + id + " not found"));
	}
}

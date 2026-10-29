package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.ListVehiclesUseCase;
import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.application.port.out.VehicleRepository.VehicleFilter;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListVehiclesService implements ListVehiclesUseCase {

	private final VehicleRepository vehicles;

	ListVehiclesService(VehicleRepository vehicles) {
		this.vehicles = vehicles;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Vehicle> execute(VehicleFilter filter, PageQuery page) {
		return vehicles.search(filter, page);
	}
}

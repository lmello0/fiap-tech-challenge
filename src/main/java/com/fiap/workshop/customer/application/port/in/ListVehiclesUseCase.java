package com.fiap.workshop.customer.application.port.in;

import com.fiap.workshop.customer.application.port.out.VehicleRepository.VehicleFilter;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;

public interface ListVehiclesUseCase {

	PageResult<Vehicle> execute(VehicleFilter filter, PageQuery page);
}

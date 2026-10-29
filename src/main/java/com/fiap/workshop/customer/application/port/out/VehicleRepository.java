package com.fiap.workshop.customer.application.port.out;

import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.LicensePlate;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository {

	Vehicle save(Vehicle vehicle);

	Optional<Vehicle> findById(UUID id);

	Optional<Vehicle> findByPlate(LicensePlate plate);

	boolean existsByPlate(LicensePlate plate);

	PageResult<Vehicle> search(VehicleFilter filter, PageQuery page);

	record VehicleFilter(UUID customerId, boolean includeInactive) {
	}
}

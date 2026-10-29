package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.LicensePlate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryVehicleRepository implements VehicleRepository {

	final Map<UUID, Vehicle> store = new LinkedHashMap<>();

	@Override
	public Vehicle save(Vehicle vehicle) {
		store.put(vehicle.id(), vehicle);
		return vehicle;
	}

	@Override
	public Optional<Vehicle> findById(UUID id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public Optional<Vehicle> findByPlate(LicensePlate plate) {
		return store.values().stream().filter(v -> v.plate().equals(plate)).findFirst();
	}

	@Override
	public boolean existsByPlate(LicensePlate plate) {
		return findByPlate(plate).isPresent();
	}

	@Override
	public PageResult<Vehicle> search(VehicleFilter filter, PageQuery page) {
		List<Vehicle> all = store.values().stream()
				.filter(v -> filter.includeInactive() || v.active())
				.filter(v -> filter.customerId() == null || v.customerId().equals(filter.customerId()))
				.toList();
		return new PageResult<>(all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList(), page.page(),
				page.size(), all.size());
	}
}

package com.fiap.workshop.customer.adapter.out.persistence;

import com.fiap.workshop.customer.application.port.out.VehicleRepository;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.LicensePlate;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class VehicleRepositoryAdapter implements VehicleRepository {

	private final VehicleJpaRepository jpa;

	VehicleRepositoryAdapter(VehicleJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Vehicle save(Vehicle vehicle) {
		VehicleEntity entity = jpa.findById(vehicle.id()).orElseGet(VehicleEntity::new);
		entity.setId(vehicle.id());
		entity.setCustomerId(vehicle.customerId());
		entity.setPlate(vehicle.plate().value());
		entity.setBrand(vehicle.brand());
		entity.setModel(vehicle.model());
		entity.setYear(vehicle.year());
		entity.setActive(vehicle.active());
		entity.setCreatedAt(vehicle.createdAt());
		entity.setUpdatedAt(vehicle.updatedAt());
		return toDomain(jpa.saveAndFlush(entity));
	}

	@Override
	public Optional<Vehicle> findById(UUID id) {
		return jpa.findById(id).map(VehicleRepositoryAdapter::toDomain);
	}

	@Override
	public Optional<Vehicle> findByPlate(LicensePlate plate) {
		return jpa.findByPlate(plate.value()).map(VehicleRepositoryAdapter::toDomain);
	}

	@Override
	public boolean existsByPlate(LicensePlate plate) {
		return jpa.existsByPlate(plate.value());
	}

	@Override
	public PageResult<Vehicle> search(VehicleFilter filter, PageQuery page) {
		Specification<VehicleEntity> spec = (root, query, cb) -> {
			var predicates = new ArrayList<Predicate>();
			if (!filter.includeInactive()) {
				predicates.add(cb.isTrue(root.get("active")));
			}
			if (filter.customerId() != null) {
				predicates.add(cb.equal(root.get("customerId"), filter.customerId()));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		Page<VehicleEntity> result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(), Sort.by("plate", "id")));
		return new PageResult<>(result.map(VehicleRepositoryAdapter::toDomain).getContent(), page.page(), page.size(),
				result.getTotalElements());
	}

	private static Vehicle toDomain(VehicleEntity e) {
		return Vehicle.restore(e.getId(), e.getCustomerId(), LicensePlate.of(e.getPlate()), e.getBrand(), e.getModel(),
				e.getYear(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
	}
}

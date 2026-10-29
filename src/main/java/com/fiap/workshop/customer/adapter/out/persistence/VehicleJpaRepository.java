package com.fiap.workshop.customer.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface VehicleJpaRepository extends JpaRepository<VehicleEntity, UUID>, JpaSpecificationExecutor<VehicleEntity> {

	Optional<VehicleEntity> findByPlate(String plate);

	boolean existsByPlate(String plate);
}

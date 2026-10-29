package com.fiap.workshop.catalog.adapter.out.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface RepairServiceJpaRepository extends JpaRepository<RepairServiceEntity, UUID>, JpaSpecificationExecutor<RepairServiceEntity> {

	boolean existsByNameIgnoreCase(String name);

	boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}

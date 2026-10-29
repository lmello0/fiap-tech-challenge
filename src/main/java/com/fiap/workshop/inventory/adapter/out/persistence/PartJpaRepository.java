package com.fiap.workshop.inventory.adapter.out.persistence;

import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PartJpaRepository extends JpaRepository<PartEntity, UUID>, JpaSpecificationExecutor<PartEntity> {

	boolean existsBySku(String sku);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from PartEntity p where p.id in :ids order by p.id")
	List<PartEntity> findAllForUpdate(@Param("ids") Collection<UUID> ids);
}

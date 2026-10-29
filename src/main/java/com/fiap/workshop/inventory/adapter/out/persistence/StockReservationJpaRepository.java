package com.fiap.workshop.inventory.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockReservationJpaRepository extends JpaRepository<StockReservationEntity, UUID> {

	Optional<StockReservationEntity> findByBudgetId(UUID budgetId);

	List<StockReservationEntity> findByWorkOrderId(UUID workOrderId);

	@Query("select distinct r from StockReservationEntity r join r.lines l "
			+ "where r.status = com.fiap.workshop.inventory.domain.ReservationStatus.WAITING and l.partId in :partIds "
			+ "order by r.createdAt, r.id")
	List<StockReservationEntity> findWaitingForParts(@Param("partIds") Collection<UUID> partIds);
}

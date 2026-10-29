package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.domain.ItemType;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkOrderJpaRepository extends JpaRepository<WorkOrderEntity, UUID>, JpaSpecificationExecutor<WorkOrderEntity> {

	boolean existsByVehicleIdAndStatusNotIn(UUID vehicleId, Collection<WorkOrderStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<WorkOrderEntity> findByCustomerIdAndStatusNotInOrderById(UUID customerId, Collection<WorkOrderStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<WorkOrderEntity> findByVehicleIdAndStatusNotInOrderById(UUID vehicleId, Collection<WorkOrderStatus> statuses);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w.id from WorkOrderEntity w where w.id = :id")
	Optional<UUID> lockById(@Param("id") UUID id);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select w from WorkOrderEntity w where w.status not in :statuses and exists "
			+ "(select 1 from WorkOrderItemEntity i where i.workOrder = w and i.type = :type and i.referenceId = :referenceId) "
			+ "order by w.id")
	List<WorkOrderEntity> findWithDraftItem(@Param("type") ItemType type, @Param("referenceId") UUID referenceId,
			@Param("statuses") Collection<WorkOrderStatus> statuses);
}

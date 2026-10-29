package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.domain.ItemType;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkOrderJpaRepository extends JpaRepository<WorkOrderEntity, UUID>, JpaSpecificationExecutor<WorkOrderEntity> {

	boolean existsByVehicleIdAndStatusNotIn(UUID vehicleId, Collection<WorkOrderStatus> statuses);

	List<WorkOrderEntity> findByCustomerIdAndStatusNotIn(UUID customerId, Collection<WorkOrderStatus> statuses);

	List<WorkOrderEntity> findByVehicleIdAndStatusNotIn(UUID vehicleId, Collection<WorkOrderStatus> statuses);

	@Query("select distinct w from WorkOrderEntity w join w.items i "
			+ "where i.type = :type and i.referenceId = :referenceId and w.status not in :statuses")
	List<WorkOrderEntity> findWithDraftItem(@Param("type") ItemType type, @Param("referenceId") UUID referenceId,
			@Param("statuses") Collection<WorkOrderStatus> statuses);
}

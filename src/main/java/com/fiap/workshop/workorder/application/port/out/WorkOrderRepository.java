package com.fiap.workshop.workorder.application.port.out;

import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.workorder.domain.ItemType;
import com.fiap.workshop.workorder.domain.WorkOrder;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkOrderRepository {

	WorkOrder save(WorkOrder workOrder);

	Optional<WorkOrder> findById(UUID id);

	Optional<WorkOrder> findByIdForUpdate(UUID id);

	boolean existsActiveForVehicle(UUID vehicleId);

	List<WorkOrder> findActiveByCustomerId(UUID customerId);

	List<WorkOrder> findActiveByVehicleId(UUID vehicleId);

	List<WorkOrder> findActiveWithDraftItem(ItemType type, UUID referenceId);

	PageResult<WorkOrderSummary> search(WorkOrderFilter filter, PageQuery page);

	record WorkOrderFilter(UUID customerId, WorkOrderStatus status, String number, String plate, boolean includeFinished) {
	}
}

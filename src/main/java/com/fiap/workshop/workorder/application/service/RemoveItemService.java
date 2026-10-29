package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.workorder.application.port.in.RemoveItemUseCase;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RemoveItemService implements RemoveItemUseCase {

	private final WorkOrderSupport support;
	private final WorkOrderRepository workOrders;

	RemoveItemService(WorkOrderSupport support, WorkOrderRepository workOrders) {
		this.support = support;
		this.workOrders = workOrders;
	}

	@Override
	@Transactional
	public WorkOrder execute(UUID workOrderId, UUID itemId) {
		WorkOrder workOrder = support.load(workOrderId);
		workOrder.removeItem(itemId);
		return workOrders.save(workOrder);
	}
}

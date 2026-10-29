package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.workorder.application.port.in.ChangeItemQuantityUseCase;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.domain.WorkOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ChangeItemQuantityService implements ChangeItemQuantityUseCase {

	private final WorkOrderSupport support;
	private final WorkOrderRepository workOrders;

	ChangeItemQuantityService(WorkOrderSupport support, WorkOrderRepository workOrders) {
		this.support = support;
		this.workOrders = workOrders;
	}

	@Override
	@Transactional
	public WorkOrder execute(Command command) {
		WorkOrder workOrder = support.load(command.workOrderId());
		workOrder.changeItemQuantity(command.itemId(), command.quantity());
		return workOrders.save(workOrder);
	}
}

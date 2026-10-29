package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.workorder.application.port.in.AddItemUseCase;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.domain.NewItem;
import com.fiap.workshop.workorder.domain.WorkOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AddItemService implements AddItemUseCase {

	private final WorkOrderSupport support;
	private final WorkOrderRepository workOrders;
	private final ItemResolver items;

	AddItemService(WorkOrderSupport support, WorkOrderRepository workOrders, ItemResolver items) {
		this.support = support;
		this.workOrders = workOrders;
		this.items = items;
	}

	@Override
	@Transactional
	public WorkOrder execute(Command command) {
		WorkOrder workOrder = support.load(command.workOrderId());
		NewItem item = items.resolve(command.item());
		workOrder.addItem(item.type(), item.referenceId(), item.name(), item.unitPrice(), item.quantity());
		return workOrders.save(workOrder);
	}
}

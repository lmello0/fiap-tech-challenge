package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.workorder.application.port.in.ListWorkOrdersUseCase;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository.WorkOrderFilter;
import com.fiap.workshop.workorder.application.port.out.WorkOrderSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListWorkOrdersService implements ListWorkOrdersUseCase {

	private final WorkOrderRepository workOrders;

	ListWorkOrdersService(WorkOrderRepository workOrders) {
		this.workOrders = workOrders;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<WorkOrderSummary> execute(WorkOrderFilter filter, PageQuery page) {
		return workOrders.search(filter, page);
	}
}

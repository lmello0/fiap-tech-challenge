package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.workorder.application.port.in.GetWorkOrderUseCase;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetWorkOrderService implements GetWorkOrderUseCase {

	private final WorkOrderSupport support;

	GetWorkOrderService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional(readOnly = true)
	public WorkOrder execute(UUID workOrderId, UUID customerScope) {
		return support.loadOwned(workOrderId, customerScope);
	}
}

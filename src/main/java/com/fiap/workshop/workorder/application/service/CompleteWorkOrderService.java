package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.application.port.in.CompleteWorkOrderUseCase;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CompleteWorkOrderService implements CompleteWorkOrderUseCase {

	private final WorkOrderSupport support;

	CompleteWorkOrderService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public WorkOrder execute(UUID workOrderId, Actor actor) {
		WorkOrder workOrder = support.load(workOrderId);
		int before = workOrder.history().size();
		Instant now = support.clock().instant();
		workOrder.complete(actor, now);
		WorkOrder saved = support.save(workOrder, before);
		support.publish(WorkOrderEvents.completed(saved, now));
		return saved;
	}
}

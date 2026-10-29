package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.application.port.in.CancelWorkOrderUseCase;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CancelWorkOrderService implements CancelWorkOrderUseCase {

	private final WorkOrderSupport support;

	CancelWorkOrderService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public WorkOrder execute(UUID workOrderId, String reason, Actor actor) {
		WorkOrder workOrder = support.load(workOrderId);
		int before = workOrder.history().size();
		Instant now = support.clock().instant();
		workOrder.cancel(reason, actor, now);
		WorkOrder saved = support.save(workOrder, before);
		support.publish(WorkOrderEvents.cancelled(saved, reason, now));
		return saved;
	}
}

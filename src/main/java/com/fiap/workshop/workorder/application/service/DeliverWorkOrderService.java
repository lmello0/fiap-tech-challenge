package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.workorder.application.port.in.DeliverWorkOrderUseCase;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeliverWorkOrderService implements DeliverWorkOrderUseCase {

	private final WorkOrderSupport support;

	DeliverWorkOrderService(WorkOrderSupport support) {
		this.support = support;
	}

	@Override
	@Transactional
	public WorkOrder execute(UUID workOrderId, Actor actor) {
		WorkOrder workOrder = support.load(workOrderId);
		int before = workOrder.history().size();
		Instant now = support.clock().instant();
		workOrder.deliver(actor, now);
		WorkOrder saved = support.save(workOrder, before);
		support.publish(WorkOrderEvents.delivered(saved, now));
		return saved;
	}
}

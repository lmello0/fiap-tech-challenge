package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.NotFoundException;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class WorkOrderSupport {

	private final WorkOrderRepository workOrders;
	private final EventPublisher events;
	private final Clock clock;

	WorkOrderSupport(WorkOrderRepository workOrders, EventPublisher events, Clock clock) {
		this.workOrders = workOrders;
		this.events = events;
		this.clock = clock;
	}

	WorkOrder load(UUID id) {
		return workOrders.findById(id).orElseThrow(() -> new NotFoundException("Work order " + id + " not found"));
	}

	WorkOrder loadForUpdate(UUID id) {
		return workOrders.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Work order " + id + " not found"));
	}

	WorkOrder loadOwned(UUID id, UUID customerScope) {
		WorkOrder workOrder = load(id);
		if (customerScope != null && !workOrder.customer().id().equals(customerScope)) {
			throw new NotFoundException("Work order " + id + " not found");
		}
		return workOrder;
	}

	WorkOrder save(WorkOrder workOrder, int historySizeBefore) {
		WorkOrder saved = workOrders.save(workOrder);
		saved.history().stream().skip(historySizeBefore)
				.forEach(change -> events.publish(WorkOrderEvents.statusChanged(saved, change)));
		return saved;
	}

	void publish(Object event) {
		events.publish(event);
	}

	Clock clock() {
		return clock;
	}
}

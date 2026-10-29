package com.fiap.workshop.notification.adapter.in.event;

import com.fiap.workshop.inventory.api.events.LowStockDetected;
import com.fiap.workshop.notification.application.port.in.NotifyBudgetIssuedUseCase;
import com.fiap.workshop.notification.application.port.in.NotifyLowStockUseCase;
import com.fiap.workshop.notification.application.port.in.NotifyStatusChangedUseCase;
import com.fiap.workshop.notification.application.port.in.NotifyWorkOrderCompletedUseCase;
import com.fiap.workshop.workorder.api.events.BudgetIssued;
import com.fiap.workshop.workorder.api.events.WorkOrderCompleted;
import com.fiap.workshop.workorder.api.events.WorkOrderStatusChanged;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class IntegrationEventsListener {

	private final NotifyBudgetIssuedUseCase budgetIssued;
	private final NotifyWorkOrderCompletedUseCase workOrderCompleted;
	private final NotifyLowStockUseCase lowStock;
	private final NotifyStatusChangedUseCase statusChanged;

	IntegrationEventsListener(NotifyBudgetIssuedUseCase budgetIssued, NotifyWorkOrderCompletedUseCase workOrderCompleted,
			NotifyLowStockUseCase lowStock, NotifyStatusChangedUseCase statusChanged) {
		this.budgetIssued = budgetIssued;
		this.workOrderCompleted = workOrderCompleted;
		this.lowStock = lowStock;
		this.statusChanged = statusChanged;
	}

	@ApplicationModuleListener
	void on(BudgetIssued event) {
		budgetIssued.execute(new NotifyBudgetIssuedUseCase.Command(event.eventId(), event.customerName(), event.customerEmail(),
				event.number(), event.sequence(), event.kind(), event.total(),
				event.lines().stream().map(l -> new NotifyBudgetIssuedUseCase.Line(l.type(), l.name(), l.unitPrice(), l.quantity())).toList()));
	}

	@ApplicationModuleListener
	void on(WorkOrderCompleted event) {
		workOrderCompleted.execute(new NotifyWorkOrderCompletedUseCase.Command(event.eventId(), event.customerName(),
				event.customerEmail(), event.number()));
	}

	@ApplicationModuleListener
	void on(LowStockDetected event) {
		lowStock.execute(new NotifyLowStockUseCase.Command(event.eventId(), event.sku(), event.name(), event.available(),
				event.minimumQuantity()));
	}

	@ApplicationModuleListener
	void on(WorkOrderStatusChanged event) {
		statusChanged.execute(new NotifyStatusChangedUseCase.Command(event.eventId(), event.customerName(), event.customerEmail(),
				event.number(), event.toStatus()));
	}
}

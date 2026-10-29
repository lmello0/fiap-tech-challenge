package com.fiap.workshop.inventory.adapter.in.event;

import com.fiap.workshop.inventory.application.port.in.ConsumeStockUseCase;
import com.fiap.workshop.inventory.application.port.in.PartQuantity;
import com.fiap.workshop.inventory.application.port.in.ReleaseStockUseCase;
import com.fiap.workshop.inventory.application.port.in.ReserveStockUseCase;
import com.fiap.workshop.workorder.api.events.BudgetApproved;
import com.fiap.workshop.workorder.api.events.WorkOrderCancelled;
import com.fiap.workshop.workorder.api.events.WorkOrderCompleted;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class WorkOrderEventsListener {

	private final ReserveStockUseCase reserve;
	private final ConsumeStockUseCase consume;
	private final ReleaseStockUseCase release;

	WorkOrderEventsListener(ReserveStockUseCase reserve, ConsumeStockUseCase consume, ReleaseStockUseCase release) {
		this.reserve = reserve;
		this.consume = consume;
		this.release = release;
	}

	@ApplicationModuleListener
	void on(BudgetApproved event) {
		reserve.execute(new ReserveStockUseCase.Command(event.workOrderId(), event.number(), event.budgetId(),
				event.parts().stream().map(p -> new PartQuantity(p.partId(), p.quantity())).toList()));
	}

	@ApplicationModuleListener
	void on(WorkOrderCompleted event) {
		consume.execute(event.workOrderId());
	}

	@ApplicationModuleListener
	void on(WorkOrderCancelled event) {
		release.execute(event.workOrderId());
	}
}

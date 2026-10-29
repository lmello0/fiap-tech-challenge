package com.fiap.workshop.workorder.adapter.in.event;

import com.fiap.workshop.inventory.api.events.PartsReserved;
import com.fiap.workshop.inventory.api.events.PartsShortage;
import com.fiap.workshop.workorder.application.port.in.StartExecutionUseCase;
import com.fiap.workshop.workorder.application.port.in.WaitForPartsUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class StockEventsListener {

	private final StartExecutionUseCase startExecution;
	private final WaitForPartsUseCase waitForParts;

	StockEventsListener(StartExecutionUseCase startExecution, WaitForPartsUseCase waitForParts) {
		this.startExecution = startExecution;
		this.waitForParts = waitForParts;
	}

	@ApplicationModuleListener
	void on(PartsReserved event) {
		startExecution.execute(event.workOrderId(), event.budgetId());
	}

	@ApplicationModuleListener
	void on(PartsShortage event) {
		waitForParts.execute(event.workOrderId(), event.budgetId());
	}
}

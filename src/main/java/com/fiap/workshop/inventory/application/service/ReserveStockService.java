package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.ReserveStockUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReserveStockService implements ReserveStockUseCase {

	private final StockReservations reservations;

	ReserveStockService(StockReservations reservations) {
		this.reservations = reservations;
	}

	@Override
	@Transactional
	public void execute(Command command) {
		if (command.parts().isEmpty()) {
			return;
		}
		reservations.request(command.workOrderId(), command.workOrderNumber(), command.budgetId(), command.parts());
	}
}

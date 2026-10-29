package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.ConsumeStockUseCase;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ConsumeStockService implements ConsumeStockUseCase {

	private final StockReservations reservations;

	ConsumeStockService(StockReservations reservations) {
		this.reservations = reservations;
	}

	@Override
	@Transactional
	public void execute(UUID workOrderId) {
		reservations.consume(workOrderId);
	}
}

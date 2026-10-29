package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.ReleaseStockUseCase;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ReleaseStockService implements ReleaseStockUseCase {

	private final StockReservations reservations;

	ReleaseStockService(StockReservations reservations) {
		this.reservations = reservations;
	}

	@Override
	@Transactional
	public void execute(UUID workOrderId) {
		reservations.release(workOrderId);
	}
}

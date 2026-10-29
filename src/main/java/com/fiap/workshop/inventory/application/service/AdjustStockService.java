package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.AdjustStockUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.application.port.out.StockMovementRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.inventory.domain.StockMovement;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class AdjustStockService implements AdjustStockUseCase {

	private final PartRepository parts;
	private final StockMovementRepository movements;
	private final EventPublisher events;
	private final StockReservations reservations;
	private final Clock clock;

	AdjustStockService(PartRepository parts, StockMovementRepository movements, EventPublisher events, StockReservations reservations,
			Clock clock) {
		this.parts = parts;
		this.movements = movements;
		this.events = events;
		this.reservations = reservations;
		this.clock = clock;
	}

	@Override
	@Transactional
	public StockMovement execute(Command command) {
		Part part = parts.findById(command.partId())
				.orElseThrow(() -> new NotFoundException("Part " + command.partId() + " not found"));
		int previousAvailable = part.available();
		StockMovement movement = part.adjust(command.delta(), command.reason(), command.performedBy(), clock.instant());
		parts.save(part);
		movements.save(movement);
		if (part.crossedLowStockFrom(previousAvailable)) {
			events.publish(InventoryEvents.lowStock(part, clock.instant()));
		}
		if (command.delta() > 0) {
			reservations.retryWaiting(java.util.Set.of(part.id()));
		}
		return movement;
	}
}

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
	private final Clock clock;

	AdjustStockService(PartRepository parts, StockMovementRepository movements, EventPublisher events, Clock clock) {
		this.parts = parts;
		this.movements = movements;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public StockMovement execute(Command command) {
		Part part = parts.findById(command.partId())
				.orElseThrow(() -> new NotFoundException("Part " + command.partId() + " not found"));
		int previousOnHand = part.onHand();
		StockMovement movement = part.adjust(command.delta(), command.reason(), command.performedBy(), clock.instant());
		parts.save(part);
		movements.save(movement);
		if (part.crossedLowStockFrom(previousOnHand)) {
			events.publish(InventoryEvents.lowStock(part, clock.instant()));
		}
		return movement;
	}
}

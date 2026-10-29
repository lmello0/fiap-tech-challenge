package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.RegisterStockEntryUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.application.port.out.StockMovementRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.inventory.domain.StockMovement;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterStockEntryService implements RegisterStockEntryUseCase {

	private final PartRepository parts;
	private final StockMovementRepository movements;
	private final Clock clock;

	RegisterStockEntryService(PartRepository parts, StockMovementRepository movements, Clock clock) {
		this.parts = parts;
		this.movements = movements;
		this.clock = clock;
	}

	@Override
	@Transactional
	public StockMovement execute(Command command) {
		Part part = parts.findById(command.partId())
				.orElseThrow(() -> new NotFoundException("Part " + command.partId() + " not found"));
		StockMovement movement = part.receive(command.quantity(), command.note(), command.performedBy(), clock.instant());
		parts.save(part);
		return movements.save(movement);
	}
}

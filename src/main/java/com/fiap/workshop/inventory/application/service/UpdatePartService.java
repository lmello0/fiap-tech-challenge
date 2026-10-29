package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.UpdatePartUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdatePartService implements UpdatePartUseCase {

	private final PartRepository parts;
	private final EventPublisher events;
	private final Clock clock;

	UpdatePartService(PartRepository parts, EventPublisher events, Clock clock) {
		this.parts = parts;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Part execute(Command command) {
		Part part = parts.findById(command.id()).orElseThrow(() -> new NotFoundException("Part " + command.id() + " not found"));
		boolean wasLow = part.lowStock();
		part.update(command.name(), command.description(), command.unitPrice() == null ? null : new Money(command.unitPrice()),
				command.minimumQuantity(), clock.instant());
		parts.save(part);
		events.publish(InventoryEvents.updated(part, clock.instant()));
		if (!wasLow && part.lowStock()) {
			events.publish(InventoryEvents.lowStock(part, clock.instant()));
		}
		return part;
	}
}

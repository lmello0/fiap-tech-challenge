package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.CreatePartUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.Money;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreatePartService implements CreatePartUseCase {

	private final PartRepository parts;
	private final EventPublisher events;
	private final Clock clock;

	CreatePartService(PartRepository parts, EventPublisher events, Clock clock) {
		this.parts = parts;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Part execute(Command command) {
		Part part = Part.create(command.sku(), command.name(), command.description(),
				command.unitPrice() == null ? null : new Money(command.unitPrice()), command.minimumQuantity(), clock.instant());
		if (parts.existsBySku(part.sku())) {
			throw new ConflictException("A part with this SKU already exists");
		}
		parts.save(part);
		events.publish(InventoryEvents.created(part, clock.instant()));
		return part;
	}
}

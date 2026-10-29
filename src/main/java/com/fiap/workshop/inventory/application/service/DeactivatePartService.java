package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.application.port.in.DeactivatePartUseCase;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeactivatePartService implements DeactivatePartUseCase {

	private final PartRepository parts;
	private final EventPublisher events;
	private final Clock clock;

	DeactivatePartService(PartRepository parts, EventPublisher events, Clock clock) {
		this.parts = parts;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void execute(UUID id) {
		Part part = parts.findById(id).orElseThrow(() -> new NotFoundException("Part " + id + " not found"));
		part.deactivate(clock.instant());
		parts.save(part);
		events.publish(InventoryEvents.deactivated(part, clock.instant()));
	}
}

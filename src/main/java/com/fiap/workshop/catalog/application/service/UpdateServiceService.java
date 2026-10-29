package com.fiap.workshop.catalog.application.service;

import com.fiap.workshop.catalog.application.port.in.UpdateServiceUseCase;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateServiceService implements UpdateServiceUseCase {

	private final RepairServiceRepository services;
	private final EventPublisher events;
	private final Clock clock;

	UpdateServiceService(RepairServiceRepository services, EventPublisher events, Clock clock) {
		this.services = services;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public RepairService execute(Command command) {
		RepairService service = services.findById(command.id())
				.orElseThrow(() -> new NotFoundException("Service " + command.id() + " not found"));
		service.update(command.name(), command.description(), command.price() == null ? null : new Money(command.price()),
				command.estimatedMinutes(), clock.instant());
		if (services.existsByNameAndIdNot(service.name(), service.id())) {
			throw new ConflictException("A service with this name already exists");
		}
		services.save(service);
		events.publish(CatalogEvents.updated(service, clock.instant()));
		return service;
	}
}

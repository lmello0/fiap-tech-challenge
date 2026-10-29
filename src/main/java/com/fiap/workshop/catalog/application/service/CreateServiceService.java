package com.fiap.workshop.catalog.application.service;

import com.fiap.workshop.catalog.application.port.in.CreateServiceUseCase;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.Money;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CreateServiceService implements CreateServiceUseCase {

	private final RepairServiceRepository services;
	private final EventPublisher events;
	private final Clock clock;

	CreateServiceService(RepairServiceRepository services, EventPublisher events, Clock clock) {
		this.services = services;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public RepairService execute(Command command) {
		RepairService service = RepairService.create(command.name(), command.description(),
				command.price() == null ? null : new Money(command.price()), command.estimatedMinutes(), clock.instant());
		if (services.existsByName(service.name())) {
			throw new ConflictException("A service with this name already exists");
		}
		services.save(service);
		events.publish(CatalogEvents.created(service, clock.instant()));
		return service;
	}
}

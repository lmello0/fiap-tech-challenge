package com.fiap.workshop.catalog.application.service;

import com.fiap.workshop.catalog.application.port.in.DeactivateServiceUseCase;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class DeactivateServiceService implements DeactivateServiceUseCase {

	private final RepairServiceRepository services;
	private final EventPublisher events;
	private final Clock clock;

	DeactivateServiceService(RepairServiceRepository services, EventPublisher events, Clock clock) {
		this.services = services;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public void execute(UUID id) {
		RepairService service = services.findById(id).orElseThrow(() -> new NotFoundException("Service " + id + " not found"));
		service.deactivate(clock.instant());
		services.save(service);
		events.publish(CatalogEvents.deactivated(service, clock.instant()));
	}
}

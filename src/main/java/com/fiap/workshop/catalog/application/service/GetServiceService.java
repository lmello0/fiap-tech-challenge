package com.fiap.workshop.catalog.application.service;

import com.fiap.workshop.catalog.application.port.in.GetServiceUseCase;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetServiceService implements GetServiceUseCase {

	private final RepairServiceRepository services;

	GetServiceService(RepairServiceRepository services) {
		this.services = services;
	}

	@Override
	@Transactional(readOnly = true)
	public RepairService execute(UUID id) {
		return services.findById(id).orElseThrow(() -> new NotFoundException("Service " + id + " not found"));
	}
}

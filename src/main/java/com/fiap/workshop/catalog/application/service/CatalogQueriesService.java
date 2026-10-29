package com.fiap.workshop.catalog.application.service;

import com.fiap.workshop.catalog.api.CatalogQueries;
import com.fiap.workshop.catalog.api.ServiceView;
import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CatalogQueriesService implements CatalogQueries {

	private final RepairServiceRepository services;

	CatalogQueriesService(RepairServiceRepository services) {
		this.services = services;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<ServiceView> findServiceById(UUID id) {
		return services.findById(id).map(s -> new ServiceView(s.id(), s.name(), s.price().amount(), s.estimatedMinutes(), s.active()));
	}
}

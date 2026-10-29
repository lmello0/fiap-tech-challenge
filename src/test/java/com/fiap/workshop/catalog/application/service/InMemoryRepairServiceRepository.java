package com.fiap.workshop.catalog.application.service;

import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryRepairServiceRepository implements RepairServiceRepository {

	final Map<UUID, RepairService> store = new LinkedHashMap<>();

	@Override
	public RepairService save(RepairService service) {
		store.put(service.id(), service);
		return service;
	}

	@Override
	public Optional<RepairService> findById(UUID id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public boolean existsByName(String name) {
		return store.values().stream().anyMatch(s -> s.name().equalsIgnoreCase(name));
	}

	@Override
	public boolean existsByNameAndIdNot(String name, UUID id) {
		return store.values().stream().anyMatch(s -> s.name().equalsIgnoreCase(name) && !s.id().equals(id));
	}

	@Override
	public PageResult<RepairService> search(ServiceFilter filter, PageQuery page) {
		List<RepairService> all = store.values().stream()
				.filter(s -> filter.includeInactive() || s.active())
				.filter(s -> filter.name() == null || s.name().toLowerCase().contains(filter.name().toLowerCase()))
				.toList();
		return new PageResult<>(all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList(), page.page(),
				page.size(), all.size());
	}
}

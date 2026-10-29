package com.fiap.workshop.catalog.application.port.out;

import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface RepairServiceRepository {

	RepairService save(RepairService service);

	Optional<RepairService> findById(UUID id);

	boolean existsByName(String name);

	boolean existsByNameAndIdNot(String name, UUID id);

	PageResult<RepairService> search(ServiceFilter filter, PageQuery page);

	record ServiceFilter(String name, boolean includeInactive) {
	}
}

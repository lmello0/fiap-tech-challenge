package com.fiap.workshop.inventory.application.port.out;

import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import java.util.Optional;
import java.util.UUID;

public interface PartRepository {

	Part save(Part part);

	Optional<Part> findById(UUID id);

	boolean existsBySku(String sku);

	PageResult<Part> search(PartFilter filter, PageQuery page);

	record PartFilter(String text, boolean lowStockOnly, boolean includeInactive) {
	}
}

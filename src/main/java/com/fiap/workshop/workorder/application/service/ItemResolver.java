package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.NotFoundException;
import com.fiap.workshop.workorder.application.port.in.ItemRequest;
import com.fiap.workshop.workorder.application.port.out.ItemCatalog;
import com.fiap.workshop.workorder.application.port.out.ItemCatalog.ItemInfo;
import com.fiap.workshop.workorder.domain.NewItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class ItemResolver {

	private final ItemCatalog catalog;

	ItemResolver(ItemCatalog catalog) {
		this.catalog = catalog;
	}

	NewItem resolve(ItemRequest request) {
		ItemInfo info = catalog.find(request.type(), request.referenceId())
				.orElseThrow(() -> new NotFoundException(request.type() + " " + request.referenceId() + " not found"));
		if (!info.active()) {
			throw new DomainException(request.type() + " " + info.name() + " is deactivated");
		}
		return new NewItem(request.type(), info.id(), info.name(), info.unitPrice(), request.quantity());
	}

	List<NewItem> resolveAll(List<ItemRequest> requests) {
		return requests == null ? List.of() : requests.stream().map(this::resolve).toList();
	}
}

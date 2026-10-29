package com.fiap.workshop.workorder.application.port.out;

import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.workorder.domain.ItemType;
import java.util.Optional;
import java.util.UUID;

public interface ItemCatalog {

	Optional<ItemInfo> find(ItemType type, UUID id);

	record ItemInfo(ItemType type, UUID id, String name, Money unitPrice, boolean active) {
	}
}

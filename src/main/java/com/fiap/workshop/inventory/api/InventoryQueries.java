package com.fiap.workshop.inventory.api;

import java.util.Optional;
import java.util.UUID;

public interface InventoryQueries {

	Optional<PartView> findPartById(UUID id);
}

package com.fiap.workshop.catalog.api;

import java.util.Optional;
import java.util.UUID;

public interface CatalogQueries {

	Optional<ServiceView> findServiceById(UUID id);
}

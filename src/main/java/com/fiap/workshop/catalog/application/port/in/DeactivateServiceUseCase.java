package com.fiap.workshop.catalog.application.port.in;

import java.util.UUID;

public interface DeactivateServiceUseCase {

	void execute(UUID id);
}

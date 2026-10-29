package com.fiap.workshop.inventory.application.port.in;

import java.util.UUID;

public interface DeactivatePartUseCase {

	void execute(UUID id);
}

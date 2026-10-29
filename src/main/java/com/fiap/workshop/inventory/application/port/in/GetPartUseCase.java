package com.fiap.workshop.inventory.application.port.in;

import com.fiap.workshop.inventory.domain.Part;
import java.util.UUID;

public interface GetPartUseCase {

	Part execute(UUID id);
}

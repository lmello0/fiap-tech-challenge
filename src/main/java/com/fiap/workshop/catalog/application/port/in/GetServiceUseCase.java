package com.fiap.workshop.catalog.application.port.in;

import com.fiap.workshop.catalog.domain.RepairService;
import java.util.UUID;

public interface GetServiceUseCase {

	RepairService execute(UUID id);
}

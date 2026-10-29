package com.fiap.workshop.customer.application.port.in;

import com.fiap.workshop.customer.domain.Vehicle;
import java.util.UUID;

public interface GetVehicleUseCase {

	Vehicle execute(UUID id);
}

package com.fiap.workshop.customer.application.port.in;

import com.fiap.workshop.customer.domain.Customer;
import java.util.UUID;

public interface GetCustomerUseCase {

	Customer execute(UUID id);
}

package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.GetCustomerUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class GetCustomerService implements GetCustomerUseCase {

	private final CustomerRepository customers;

	GetCustomerService(CustomerRepository customers) {
		this.customers = customers;
	}

	@Override
	@Transactional(readOnly = true)
	public Customer execute(UUID id) {
		return customers.findById(id).orElseThrow(() -> new NotFoundException("Customer " + id + " not found"));
	}
}

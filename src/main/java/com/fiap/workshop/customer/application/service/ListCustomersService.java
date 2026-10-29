package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.ListCustomersUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.application.port.out.CustomerRepository.CustomerFilter;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ListCustomersService implements ListCustomersUseCase {

	private final CustomerRepository customers;

	ListCustomersService(CustomerRepository customers) {
		this.customers = customers;
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<Customer> execute(CustomerFilter filter, PageQuery page) {
		return customers.search(filter, page);
	}
}

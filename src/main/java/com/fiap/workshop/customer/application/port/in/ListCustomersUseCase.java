package com.fiap.workshop.customer.application.port.in;

import com.fiap.workshop.customer.application.port.out.CustomerRepository.CustomerFilter;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;

public interface ListCustomersUseCase {

	PageResult<Customer> execute(CustomerFilter filter, PageQuery page);
}

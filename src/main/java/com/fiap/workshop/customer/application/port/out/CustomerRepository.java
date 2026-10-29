package com.fiap.workshop.customer.application.port.out;

import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.Document;
import com.fiap.workshop.shared.domain.Email;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository {

	Customer save(Customer customer);

	Optional<Customer> findById(UUID id);

	Optional<Customer> findByDocument(Document document);

	boolean existsByDocument(Document document);

	boolean existsByEmail(Email email);

	boolean existsByEmailAndIdNot(Email email, UUID id);

	PageResult<Customer> search(CustomerFilter filter, PageQuery page);

	record CustomerFilter(String name, String document, boolean includeInactive) {
	}
}

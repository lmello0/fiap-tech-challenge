package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.Document;
import com.fiap.workshop.shared.domain.Email;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

class InMemoryCustomerRepository implements CustomerRepository {

	final Map<UUID, Customer> store = new LinkedHashMap<>();

	@Override
	public Customer save(Customer customer) {
		store.put(customer.id(), customer);
		return customer;
	}

	@Override
	public Optional<Customer> findById(UUID id) {
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public Optional<Customer> findByDocument(Document document) {
		return store.values().stream().filter(c -> c.document().equals(document)).findFirst();
	}

	@Override
	public boolean existsByDocument(Document document) {
		return findByDocument(document).isPresent();
	}

	@Override
	public boolean existsByEmail(Email email) {
		return store.values().stream().anyMatch(c -> c.email().equals(email));
	}

	@Override
	public boolean existsByEmailAndIdNot(Email email, UUID id) {
		return store.values().stream().anyMatch(c -> c.email().equals(email) && !c.id().equals(id));
	}

	@Override
	public PageResult<Customer> search(CustomerFilter filter, PageQuery page) {
		List<Customer> all = store.values().stream().filter(c -> filter.includeInactive() || c.active()).toList();
		return new PageResult<>(all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList(), page.page(),
				page.size(), all.size());
	}
}

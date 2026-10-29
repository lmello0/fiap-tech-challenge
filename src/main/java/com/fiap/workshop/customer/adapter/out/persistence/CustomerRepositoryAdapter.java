package com.fiap.workshop.customer.adapter.out.persistence;

import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.Document;
import com.fiap.workshop.shared.domain.Email;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class CustomerRepositoryAdapter implements CustomerRepository {

	private final CustomerJpaRepository jpa;

	CustomerRepositoryAdapter(CustomerJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Customer save(Customer customer) {
		CustomerEntity entity = jpa.findById(customer.id()).orElseGet(CustomerEntity::new);
		entity.setId(customer.id());
		entity.setName(customer.name());
		entity.setDocument(customer.document().value());
		entity.setEmail(customer.email().value());
		entity.setPhone(customer.phone());
		entity.setActive(customer.active());
		entity.setCreatedAt(customer.createdAt());
		entity.setUpdatedAt(customer.updatedAt());
		return toDomain(jpa.saveAndFlush(entity));
	}

	@Override
	public Optional<Customer> findById(UUID id) {
		return jpa.findById(id).map(CustomerRepositoryAdapter::toDomain);
	}

	@Override
	public Optional<Customer> findByDocument(Document document) {
		return jpa.findByDocument(document.value()).map(CustomerRepositoryAdapter::toDomain);
	}

	@Override
	public boolean existsByDocument(Document document) {
		return jpa.existsByDocument(document.value());
	}

	@Override
	public boolean existsByEmail(Email email) {
		return jpa.existsByEmail(email.value());
	}

	@Override
	public boolean existsByEmailAndIdNot(Email email, UUID id) {
		return jpa.existsByEmailAndIdNot(email.value(), id);
	}

	@Override
	public PageResult<Customer> search(CustomerFilter filter, PageQuery page) {
		Specification<CustomerEntity> spec = (root, query, cb) -> {
			var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
			if (!filter.includeInactive()) {
				predicates.add(cb.isTrue(root.get("active")));
			}
			if (filter.name() != null && !filter.name().isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.name().trim().toLowerCase() + "%"));
			}
			if (filter.document() != null && !filter.document().isBlank()) {
				predicates.add(cb.equal(root.get("document"), filter.document().replaceAll("\\D", "")));
			}
			return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};
		Page<CustomerEntity> result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(), Sort.by("name", "id")));
		return new PageResult<>(result.map(CustomerRepositoryAdapter::toDomain).getContent(), page.page(), page.size(),
				result.getTotalElements());
	}

	private static Customer toDomain(CustomerEntity e) {
		return Customer.restore(e.getId(), e.getName(), Document.of(e.getDocument()), Email.of(e.getEmail()), e.getPhone(),
				e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
	}
}

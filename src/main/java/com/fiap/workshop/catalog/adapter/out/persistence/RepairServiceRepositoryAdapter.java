package com.fiap.workshop.catalog.adapter.out.persistence;

import com.fiap.workshop.catalog.application.port.out.RepairServiceRepository;
import com.fiap.workshop.catalog.domain.RepairService;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.Money;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class RepairServiceRepositoryAdapter implements RepairServiceRepository {

	private final RepairServiceJpaRepository jpa;

	RepairServiceRepositoryAdapter(RepairServiceJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public RepairService save(RepairService service) {
		RepairServiceEntity entity = jpa.findById(service.id()).orElseGet(RepairServiceEntity::new);
		entity.setId(service.id());
		entity.setName(service.name());
		entity.setDescription(service.description());
		entity.setPrice(service.price().amount());
		entity.setEstimatedMinutes(service.estimatedMinutes());
		entity.setActive(service.active());
		entity.setCreatedAt(service.createdAt());
		entity.setUpdatedAt(service.updatedAt());
		return toDomain(jpa.saveAndFlush(entity));
	}

	@Override
	public Optional<RepairService> findById(UUID id) {
		return jpa.findById(id).map(RepairServiceRepositoryAdapter::toDomain);
	}

	@Override
	public boolean existsByName(String name) {
		return jpa.existsByNameIgnoreCase(name);
	}

	@Override
	public boolean existsByNameAndIdNot(String name, UUID id) {
		return jpa.existsByNameIgnoreCaseAndIdNot(name, id);
	}

	@Override
	public PageResult<RepairService> search(ServiceFilter filter, PageQuery page) {
		Specification<RepairServiceEntity> spec = (root, query, cb) -> {
			var predicates = new ArrayList<Predicate>();
			if (!filter.includeInactive()) {
				predicates.add(cb.isTrue(root.get("active")));
			}
			if (filter.name() != null && !filter.name().isBlank()) {
				predicates.add(cb.like(cb.lower(root.get("name")), "%" + filter.name().trim().toLowerCase() + "%"));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		Page<RepairServiceEntity> result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(), Sort.by("name", "id")));
		return new PageResult<>(result.map(RepairServiceRepositoryAdapter::toDomain).getContent(), page.page(), page.size(),
				result.getTotalElements());
	}

	private static RepairService toDomain(RepairServiceEntity e) {
		return RepairService.restore(e.getId(), e.getName(), e.getDescription(), new Money(e.getPrice()), e.getEstimatedMinutes(),
				e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
	}
}

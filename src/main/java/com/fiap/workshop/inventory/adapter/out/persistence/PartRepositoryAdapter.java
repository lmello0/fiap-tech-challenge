package com.fiap.workshop.inventory.adapter.out.persistence;

import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.Money;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class PartRepositoryAdapter implements PartRepository {

	private final PartJpaRepository jpa;

	PartRepositoryAdapter(PartJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public Part save(Part part) {
		PartEntity entity = jpa.findById(part.id()).orElseGet(PartEntity::new);
		entity.setId(part.id());
		entity.setSku(part.sku());
		entity.setName(part.name());
		entity.setDescription(part.description());
		entity.setUnitPrice(part.unitPrice().amount());
		entity.setMinimumQuantity(part.minimumQuantity());
		entity.setOnHand(part.onHand());
		entity.setReserved(part.reserved());
		entity.setActive(part.active());
		entity.setCreatedAt(part.createdAt());
		entity.setUpdatedAt(part.updatedAt());
		return toDomain(jpa.saveAndFlush(entity));
	}

	@Override
	public Optional<Part> findById(UUID id) {
		return jpa.findById(id).map(PartRepositoryAdapter::toDomain);
	}

	@Override
	public List<Part> findAllForUpdate(Collection<UUID> ids) {
		return jpa.findAllForUpdate(ids).stream().map(PartRepositoryAdapter::toDomain).toList();
	}

	@Override
	public boolean existsBySku(String sku) {
		return jpa.existsBySku(sku);
	}

	@Override
	public PageResult<Part> search(PartFilter filter, PageQuery page) {
		Specification<PartEntity> spec = (root, query, cb) -> {
			var predicates = new ArrayList<Predicate>();
			if (!filter.includeInactive()) {
				predicates.add(cb.isTrue(root.get("active")));
			}
			if (filter.lowStockOnly()) {
				predicates.add(cb.lessThanOrEqualTo(cb.diff(root.<Integer>get("onHand"), root.<Integer>get("reserved")), root.<Integer>get("minimumQuantity")));
			}
			if (filter.text() != null && !filter.text().isBlank()) {
				String like = "%" + filter.text().trim().toLowerCase() + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("sku")), like)));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		Page<PartEntity> result = jpa.findAll(spec, PageRequest.of(page.page(), page.size(), Sort.by("name", "id")));
		return new PageResult<>(result.map(PartRepositoryAdapter::toDomain).getContent(), page.page(), page.size(),
				result.getTotalElements());
	}

	private static Part toDomain(PartEntity e) {
		return Part.restore(e.getId(), e.getSku(), e.getName(), e.getDescription(), new Money(e.getUnitPrice()),
				e.getMinimumQuantity(), e.getOnHand(), e.getReserved(), e.isActive(), e.getCreatedAt(), e.getUpdatedAt());
	}
}

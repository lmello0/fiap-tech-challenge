package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.application.port.out.WorkOrderSummary;
import com.fiap.workshop.workorder.domain.ItemType;
import com.fiap.workshop.workorder.domain.WorkOrder;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
class WorkOrderRepositoryAdapter implements WorkOrderRepository {

	private static final Set<WorkOrderStatus> FINISHED = Set.of(WorkOrderStatus.DELIVERED, WorkOrderStatus.CANCELLED);

	private final WorkOrderJpaRepository jpa;

	WorkOrderRepositoryAdapter(WorkOrderJpaRepository jpa) {
		this.jpa = jpa;
	}

	@Override
	public WorkOrder save(WorkOrder workOrder) {
		WorkOrderEntity entity = jpa.findById(workOrder.id()).orElseGet(WorkOrderEntity::new);
		WorkOrderMapper.apply(workOrder, entity);
		return WorkOrderMapper.toDomain(jpa.saveAndFlush(entity));
	}

	@Override
	public Optional<WorkOrder> findById(UUID id) {
		return jpa.findById(id).map(WorkOrderMapper::toDomain);
	}

	@Override
	public boolean existsActiveForVehicle(UUID vehicleId) {
		return jpa.existsByVehicleIdAndStatusNotIn(vehicleId, FINISHED);
	}

	@Override
	public List<WorkOrder> findActiveByCustomerId(UUID customerId) {
		return jpa.findByCustomerIdAndStatusNotIn(customerId, FINISHED).stream().map(WorkOrderMapper::toDomain).toList();
	}

	@Override
	public List<WorkOrder> findActiveByVehicleId(UUID vehicleId) {
		return jpa.findByVehicleIdAndStatusNotIn(vehicleId, FINISHED).stream().map(WorkOrderMapper::toDomain).toList();
	}

	@Override
	public List<WorkOrder> findActiveWithDraftItem(ItemType type, UUID referenceId) {
		return jpa.findWithDraftItem(type, referenceId, FINISHED).stream().map(WorkOrderMapper::toDomain).toList();
	}

	@Override
	public PageResult<WorkOrderSummary> search(WorkOrderFilter filter, PageQuery page) {
		Specification<WorkOrderEntity> spec = (root, query, cb) -> {
			var predicates = new ArrayList<Predicate>();
			if (filter.customerId() != null) {
				predicates.add(cb.equal(root.get("customerId"), filter.customerId()));
			}
			if (filter.status() != null) {
				predicates.add(cb.equal(root.get("status"), filter.status()));
			}
			if (filter.number() != null && !filter.number().isBlank()) {
				predicates.add(cb.like(cb.upper(root.get("number")), "%" + filter.number().trim().toUpperCase() + "%"));
			}
			if (filter.plate() != null && !filter.plate().isBlank()) {
				predicates.add(cb.like(root.get("vehiclePlate"), "%" + filter.plate().replaceAll("[\\s-]", "").toUpperCase() + "%"));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		Page<WorkOrderEntity> result = jpa.findAll(spec,
				PageRequest.of(page.page(), page.size(), Sort.by(Sort.Direction.DESC, "createdAt", "id")));
		return new PageResult<>(result.map(WorkOrderRepositoryAdapter::summary).getContent(), page.page(), page.size(),
				result.getTotalElements());
	}

	private static WorkOrderSummary summary(WorkOrderEntity e) {
		return new WorkOrderSummary(e.getId(), e.getNumber(), e.getStatus(), e.getCustomerId(), e.getCustomerName(),
				e.getVehiclePlate(), e.getVehicleBrand() + " " + e.getVehicleModel(), e.getCreatedAt(), e.getUpdatedAt());
	}
}

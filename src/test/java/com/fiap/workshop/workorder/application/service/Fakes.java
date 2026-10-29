package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.PageResult;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.workorder.application.port.out.CustomerLookup;
import com.fiap.workshop.workorder.application.port.out.ItemCatalog;
import com.fiap.workshop.workorder.application.port.out.WorkOrderNumbers;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.application.port.out.WorkOrderSummary;
import com.fiap.workshop.workorder.domain.ItemType;
import com.fiap.workshop.workorder.domain.WorkOrder;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

final class Fakes {

	private Fakes() {
	}

	static class Repository implements WorkOrderRepository {

		final Map<UUID, WorkOrder> store = new LinkedHashMap<>();

		@Override
		public WorkOrder save(WorkOrder workOrder) {
			store.put(workOrder.id(), workOrder);
			return workOrder;
		}

		@Override
		public Optional<WorkOrder> findById(UUID id) {
			return Optional.ofNullable(store.get(id));
		}

		@Override
		public boolean existsActiveForVehicle(UUID vehicleId) {
			return store.values().stream().anyMatch(w -> w.vehicle().id().equals(vehicleId) && !w.status().terminal());
		}

		@Override
		public List<WorkOrder> findActiveByCustomerId(UUID customerId) {
			return store.values().stream().filter(w -> w.customer().id().equals(customerId) && !w.status().terminal()).toList();
		}

		@Override
		public List<WorkOrder> findActiveByVehicleId(UUID vehicleId) {
			return store.values().stream().filter(w -> w.vehicle().id().equals(vehicleId) && !w.status().terminal()).toList();
		}

		@Override
		public List<WorkOrder> findActiveWithDraftItem(ItemType type, UUID referenceId) {
			return store.values().stream().filter(w -> !w.status().terminal())
					.filter(w -> w.draftItems().stream().anyMatch(i -> i.type() == type && i.referenceId().equals(referenceId))).toList();
		}

		@Override
		public PageResult<WorkOrderSummary> search(WorkOrderFilter filter, PageQuery page) {
			List<WorkOrderSummary> all = store.values().stream()
					.filter(w -> filter.customerId() == null || w.customer().id().equals(filter.customerId()))
					.filter(w -> filter.status() == null || w.status() == filter.status())
					.map(w -> new WorkOrderSummary(w.id(), w.number(), w.status(), w.customer().id(), w.customer().name(),
							w.vehicle().plate(), w.vehicle().brand() + " " + w.vehicle().model(), w.createdAt(), w.updatedAt()))
					.toList();
			return new PageResult<>(all.stream().skip((long) page.page() * page.size()).limit(page.size()).toList(), page.page(),
					page.size(), all.size());
		}
	}

	static class Customers implements CustomerLookup {

		final Map<String, CustomerInfo> byDocument = new HashMap<>();
		final Map<String, VehicleInfo> byPlate = new HashMap<>();

		@Override
		public Optional<CustomerInfo> findCustomerByDocument(String document) {
			return Optional.ofNullable(byDocument.get(document));
		}

		@Override
		public Optional<VehicleInfo> findVehicleByPlate(String plate) {
			return Optional.ofNullable(byPlate.get(plate));
		}
	}

	static class Catalog implements ItemCatalog {

		final Map<UUID, ItemInfo> items = new HashMap<>();

		ItemInfo add(ItemType type, String name, String price, boolean active) {
			ItemInfo info = new ItemInfo(type, UUID.randomUUID(), name, Money.of(price), active);
			items.put(info.id(), info);
			return info;
		}

		@Override
		public Optional<ItemInfo> find(ItemType type, UUID id) {
			return Optional.ofNullable(items.get(id)).filter(i -> i.type() == type);
		}
	}

	static class Numbers implements WorkOrderNumbers {

		private final AtomicInteger counter = new AtomicInteger();

		@Override
		public String next(int year) {
			return "OS-%d-%06d".formatted(year, counter.incrementAndGet());
		}
	}

	static boolean isOpen(WorkOrderStatus status) {
		return !status.terminal();
	}
}

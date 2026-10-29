package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.workorder.application.port.in.RefreshSnapshotsUseCase;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.domain.CustomerSnapshot;
import com.fiap.workshop.workorder.domain.ItemType;
import com.fiap.workshop.workorder.domain.VehicleSnapshot;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RefreshSnapshotsService implements RefreshSnapshotsUseCase {

	private final WorkOrderRepository workOrders;

	RefreshSnapshotsService(WorkOrderRepository workOrders) {
		this.workOrders = workOrders;
	}

	@Override
	@Transactional
	public void refreshCustomer(UUID customerId, String name, String document, String email) {
		CustomerSnapshot snapshot = new CustomerSnapshot(customerId, name, document, email);
		workOrders.findActiveByCustomerId(customerId).stream().filter(wo -> wo.refreshCustomer(snapshot)).forEach(workOrders::save);
	}

	@Override
	@Transactional
	public void refreshVehicle(UUID vehicleId, String plate, String brand, String model, int year) {
		VehicleSnapshot snapshot = new VehicleSnapshot(vehicleId, plate, brand, model, year);
		workOrders.findActiveByVehicleId(vehicleId).stream().filter(wo -> wo.refreshVehicle(snapshot)).forEach(workOrders::save);
	}

	@Override
	@Transactional
	public void refreshService(UUID serviceId, String name, BigDecimal price) {
		refreshItem(ItemType.SERVICE, serviceId, name, price);
	}

	@Override
	@Transactional
	public void refreshPart(UUID partId, String name, BigDecimal price) {
		refreshItem(ItemType.PART, partId, name, price);
	}

	private void refreshItem(ItemType type, UUID referenceId, String name, BigDecimal price) {
		Money money = new Money(price);
		for (WorkOrder workOrder : workOrders.findActiveWithDraftItem(type, referenceId)) {
			if (workOrder.refreshDraftItem(type, referenceId, name, money)) {
				workOrders.save(workOrder);
			}
		}
	}
}

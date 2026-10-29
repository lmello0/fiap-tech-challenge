package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.workorder.application.port.in.OpenWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.out.CustomerRegistry;
import com.fiap.workshop.workorder.application.port.out.CustomerRegistry.CustomerInfo;
import com.fiap.workshop.workorder.application.port.out.CustomerRegistry.VehicleInfo;
import com.fiap.workshop.workorder.application.port.out.WorkOrderNumbers;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.domain.CustomerSnapshot;
import com.fiap.workshop.workorder.domain.NewItem;
import com.fiap.workshop.workorder.domain.VehicleSnapshot;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OpenWorkOrderService implements OpenWorkOrderUseCase {

	private final CustomerRegistry customers;
	private final ItemResolver items;
	private final WorkOrderNumbers numbers;
	private final WorkOrderRepository workOrders;
	private final WorkOrderSupport support;

	OpenWorkOrderService(CustomerRegistry customers, ItemResolver items, WorkOrderNumbers numbers, WorkOrderRepository workOrders,
			WorkOrderSupport support) {
		this.customers = customers;
		this.items = items;
		this.numbers = numbers;
		this.workOrders = workOrders;
		this.support = support;
	}

	@Override
	@Transactional
	public WorkOrder execute(Command command) {
		List<NewItem> resolvedItems = items.resolveAll(command.items());
		CustomerInfo customer = customers.ensureCustomer(command.customer());
		VehicleInfo vehicle = customers.ensureVehicle(customer.id(), command.vehicle());
		if (workOrders.existsActiveForVehicle(vehicle.id())) {
			throw new ConflictException("Vehicle " + vehicle.plate() + " already has an active work order");
		}
		Instant now = support.clock().instant();
		WorkOrder workOrder = WorkOrder.open(numbers.next(now.atZone(ZoneOffset.UTC).getYear()),
				new CustomerSnapshot(customer.id(), customer.name(), customer.document(), customer.email()),
				new VehicleSnapshot(vehicle.id(), vehicle.plate(), vehicle.brand(), vehicle.model(), vehicle.year()),
				command.complaint(), resolvedItems, command.actor(), now);
		WorkOrder saved = support.save(workOrder, 0);
		support.publish(WorkOrderEvents.opened(saved, now));
		return saved;
	}
}

package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.NotFoundException;
import com.fiap.workshop.workorder.application.port.in.OpenWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.out.CustomerLookup;
import com.fiap.workshop.workorder.application.port.out.CustomerLookup.CustomerInfo;
import com.fiap.workshop.workorder.application.port.out.CustomerLookup.VehicleInfo;
import com.fiap.workshop.workorder.application.port.out.WorkOrderNumbers;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository;
import com.fiap.workshop.workorder.domain.CustomerSnapshot;
import com.fiap.workshop.workorder.domain.VehicleSnapshot;
import com.fiap.workshop.workorder.domain.WorkOrder;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class OpenWorkOrderService implements OpenWorkOrderUseCase {

	private final CustomerLookup customers;
	private final ItemResolver items;
	private final WorkOrderNumbers numbers;
	private final WorkOrderRepository workOrders;
	private final WorkOrderSupport support;

	OpenWorkOrderService(CustomerLookup customers, ItemResolver items, WorkOrderNumbers numbers, WorkOrderRepository workOrders,
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
		CustomerInfo customer = customers.findCustomerByDocument(command.document())
				.orElseThrow(() -> new NotFoundException("No customer found for this document"));
		if (!customer.active()) {
			throw new DomainException("Customer " + customer.name() + " is deactivated");
		}
		VehicleInfo vehicle = customers.findVehicleByPlate(command.plate())
				.orElseThrow(() -> new NotFoundException("No vehicle found for this plate"));
		if (!vehicle.active()) {
			throw new DomainException("Vehicle " + vehicle.plate() + " is deactivated");
		}
		if (!vehicle.customerId().equals(customer.id())) {
			throw new DomainException("The vehicle does not belong to this customer");
		}
		if (workOrders.existsActiveForVehicle(vehicle.id())) {
			throw new ConflictException("Vehicle " + vehicle.plate() + " already has an active work order");
		}
		Instant now = support.clock().instant();
		WorkOrder workOrder = WorkOrder.open(numbers.next(now.atZone(ZoneOffset.UTC).getYear()),
				new CustomerSnapshot(customer.id(), customer.name(), customer.document(), customer.email()),
				new VehicleSnapshot(vehicle.id(), vehicle.plate(), vehicle.brand(), vehicle.model(), vehicle.year()),
				command.complaint(), items.resolveAll(command.items()), command.actor(), now);
		WorkOrder saved = support.save(workOrder, 0);
		support.publish(WorkOrderEvents.opened(saved, now));
		return saved;
	}
}

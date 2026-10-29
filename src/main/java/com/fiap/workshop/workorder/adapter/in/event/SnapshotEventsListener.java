package com.fiap.workshop.workorder.adapter.in.event;

import com.fiap.workshop.catalog.api.events.ServiceUpdated;
import com.fiap.workshop.customer.api.events.CustomerUpdated;
import com.fiap.workshop.customer.api.events.VehicleUpdated;
import com.fiap.workshop.inventory.api.events.PartUpdated;
import com.fiap.workshop.workorder.application.port.in.RefreshSnapshotsUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class SnapshotEventsListener {

	private final RefreshSnapshotsUseCase refresh;

	SnapshotEventsListener(RefreshSnapshotsUseCase refresh) {
		this.refresh = refresh;
	}

	@ApplicationModuleListener
	void on(CustomerUpdated event) {
		refresh.refreshCustomer(event.customerId(), event.name(), event.document(), event.email());
	}

	@ApplicationModuleListener
	void on(VehicleUpdated event) {
		refresh.refreshVehicle(event.vehicleId(), event.plate(), event.brand(), event.model(), event.year());
	}

	@ApplicationModuleListener
	void on(ServiceUpdated event) {
		refresh.refreshService(event.serviceId(), event.name(), event.price());
	}

	@ApplicationModuleListener
	void on(PartUpdated event) {
		refresh.refreshPart(event.partId(), event.name(), event.unitPrice());
	}
}

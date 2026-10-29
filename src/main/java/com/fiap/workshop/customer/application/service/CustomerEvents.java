package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.api.events.CustomerDeactivated;
import com.fiap.workshop.customer.api.events.CustomerRegistered;
import com.fiap.workshop.customer.api.events.CustomerUpdated;
import com.fiap.workshop.customer.api.events.VehicleDeactivated;
import com.fiap.workshop.customer.api.events.VehicleRegistered;
import com.fiap.workshop.customer.api.events.VehicleUpdated;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.customer.domain.Vehicle;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;

final class CustomerEvents {

	private CustomerEvents() {
	}

	static CustomerRegistered registered(Customer c, Instant now) {
		return new CustomerRegistered(UuidV7.generate(), now, c.id(), c.name(), c.document().value(), c.email().value(), c.phone());
	}

	static CustomerUpdated updated(Customer c, Instant now) {
		return new CustomerUpdated(UuidV7.generate(), now, c.id(), c.name(), c.document().value(), c.email().value(), c.phone());
	}

	static CustomerDeactivated deactivated(Customer c, Instant now) {
		return new CustomerDeactivated(UuidV7.generate(), now, c.id());
	}

	static VehicleRegistered registered(Vehicle v, Instant now) {
		return new VehicleRegistered(UuidV7.generate(), now, v.id(), v.customerId(), v.plate().value(), v.brand(), v.model(), v.year());
	}

	static VehicleUpdated updated(Vehicle v, Instant now) {
		return new VehicleUpdated(UuidV7.generate(), now, v.id(), v.customerId(), v.plate().value(), v.brand(), v.model(), v.year());
	}

	static VehicleDeactivated deactivated(Vehicle v, Instant now) {
		return new VehicleDeactivated(UuidV7.generate(), now, v.id(), v.customerId());
	}
}

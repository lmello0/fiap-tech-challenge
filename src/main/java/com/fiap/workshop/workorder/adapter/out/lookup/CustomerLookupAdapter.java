package com.fiap.workshop.workorder.adapter.out.lookup;

import com.fiap.workshop.customer.api.CustomerQueries;
import com.fiap.workshop.workorder.application.port.out.CustomerLookup;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class CustomerLookupAdapter implements CustomerLookup {

	private final CustomerQueries queries;

	CustomerLookupAdapter(CustomerQueries queries) {
		this.queries = queries;
	}

	@Override
	public Optional<CustomerInfo> findCustomerByDocument(String document) {
		return queries.findCustomerByDocument(document)
				.map(c -> new CustomerInfo(c.id(), c.name(), c.document(), c.email(), c.active()));
	}

	@Override
	public Optional<VehicleInfo> findVehicleByPlate(String plate) {
		return queries.findVehicleByPlate(plate)
				.map(v -> new VehicleInfo(v.id(), v.customerId(), v.plate(), v.brand(), v.model(), v.year(), v.active()));
	}
}

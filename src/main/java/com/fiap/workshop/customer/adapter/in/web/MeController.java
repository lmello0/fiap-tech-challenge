package com.fiap.workshop.customer.adapter.in.web;

import com.fiap.workshop.customer.application.port.in.GetCustomerUseCase;
import com.fiap.workshop.shared.domain.NotFoundException;
import com.fiap.workshop.shared.security.Access;
import com.fiap.workshop.shared.security.CurrentActor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
class MeController {

	private final GetCustomerUseCase get;
	private final CurrentActor currentActor;

	MeController(GetCustomerUseCase get, CurrentActor currentActor) {
		this.get = get;
		this.currentActor = currentActor;
	}

	@GetMapping
	@PreAuthorize(Access.CUSTOMER)
	CustomerResponse profile() {
		var customerId = currentActor.get().customerId();
		if (customerId == null) {
			throw new NotFoundException("No customer is linked to this account");
		}
		return CustomerResponse.from(get.execute(customerId));
	}
}

package com.fiap.workshop.identity.adapter.in.event;

import com.fiap.workshop.customer.api.events.CustomerDeactivated;
import com.fiap.workshop.customer.api.events.CustomerRegistered;
import com.fiap.workshop.customer.api.events.CustomerUpdated;
import com.fiap.workshop.identity.application.port.in.DisableCustomerAccountUseCase;
import com.fiap.workshop.identity.application.port.in.ProvisionCustomerAccountUseCase;
import com.fiap.workshop.identity.application.port.in.UpdateCustomerAccountUseCase;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
class CustomerEventsListener {

	private final ProvisionCustomerAccountUseCase provision;
	private final UpdateCustomerAccountUseCase update;
	private final DisableCustomerAccountUseCase disable;

	CustomerEventsListener(ProvisionCustomerAccountUseCase provision, UpdateCustomerAccountUseCase update,
			DisableCustomerAccountUseCase disable) {
		this.provision = provision;
		this.update = update;
		this.disable = disable;
	}

	@ApplicationModuleListener
	void on(CustomerRegistered event) {
		provision.execute(new ProvisionCustomerAccountUseCase.Command(event.customerId(), event.name(), event.email()));
	}

	@ApplicationModuleListener
	void on(CustomerUpdated event) {
		update.execute(new UpdateCustomerAccountUseCase.Command(event.customerId(), event.name(), event.email()));
	}

	@ApplicationModuleListener
	void on(CustomerDeactivated event) {
		disable.execute(event.customerId());
	}
}

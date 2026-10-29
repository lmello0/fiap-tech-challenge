package com.fiap.workshop.identity.application.service;

import com.fiap.workshop.identity.application.port.in.ProvisionCustomerAccountUseCase;
import com.fiap.workshop.identity.application.port.out.IdentityProvider;
import com.fiap.workshop.identity.application.port.out.IdentityProvider.CustomerAccount;
import org.springframework.stereotype.Service;

@Service
class ProvisionCustomerAccountService implements ProvisionCustomerAccountUseCase {

	private final IdentityProvider identityProvider;

	ProvisionCustomerAccountService(IdentityProvider identityProvider) {
		this.identityProvider = identityProvider;
	}

	@Override
	public void execute(Command command) {
		identityProvider.provisionCustomer(new CustomerAccount(command.customerId(), command.name(), command.email()));
	}
}

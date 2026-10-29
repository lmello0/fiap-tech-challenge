package com.fiap.workshop.identity.application.service;

import com.fiap.workshop.identity.application.port.in.UpdateCustomerAccountUseCase;
import com.fiap.workshop.identity.application.port.out.IdentityProvider;
import com.fiap.workshop.identity.application.port.out.IdentityProvider.CustomerAccount;
import org.springframework.stereotype.Service;

@Service
class UpdateCustomerAccountService implements UpdateCustomerAccountUseCase {

	private final IdentityProvider identityProvider;

	UpdateCustomerAccountService(IdentityProvider identityProvider) {
		this.identityProvider = identityProvider;
	}

	@Override
	public void execute(Command command) {
		identityProvider.updateCustomer(new CustomerAccount(command.customerId(), command.name(), command.email()));
	}
}

package com.fiap.workshop.identity.application.service;

import com.fiap.workshop.identity.application.port.in.DisableCustomerAccountUseCase;
import com.fiap.workshop.identity.application.port.out.IdentityProvider;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class DisableCustomerAccountService implements DisableCustomerAccountUseCase {

	private final IdentityProvider identityProvider;

	DisableCustomerAccountService(IdentityProvider identityProvider) {
		this.identityProvider = identityProvider;
	}

	@Override
	public void execute(UUID customerId) {
		identityProvider.disableCustomer(customerId);
	}
}

package com.fiap.workshop.identity.application.port.out;

import java.util.UUID;

public interface IdentityProvider {

	void provisionCustomer(CustomerAccount account);

	void updateCustomer(CustomerAccount account);

	void disableCustomer(UUID customerId);

	record CustomerAccount(UUID customerId, String name, String email) {
	}
}

package com.fiap.workshop.identity.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fiap.workshop.identity.application.port.in.ProvisionCustomerAccountUseCase;
import com.fiap.workshop.identity.application.port.in.UpdateCustomerAccountUseCase;
import com.fiap.workshop.identity.application.port.out.IdentityProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountServicesTest {

	private final List<String> calls = new ArrayList<>();
	private final IdentityProvider provider = new IdentityProvider() {
		@Override
		public void provisionCustomer(CustomerAccount account) {
			calls.add("provision:" + account.email());
		}

		@Override
		public void updateCustomer(CustomerAccount account) {
			calls.add("update:" + account.email());
		}

		@Override
		public void disableCustomer(UUID customerId) {
			calls.add("disable:" + customerId);
		}
	};

	@Test
	void servicesDelegateToTheIdentityProvider() {
		UUID id = UUID.randomUUID();

		new ProvisionCustomerAccountService(provider).execute(new ProvisionCustomerAccountUseCase.Command(id, "Ana", "a@b.co"));
		new UpdateCustomerAccountService(provider).execute(new UpdateCustomerAccountUseCase.Command(id, "Ana", "c@d.co"));
		new DisableCustomerAccountService(provider).execute(id);

		assertThat(calls).containsExactly("provision:a@b.co", "update:c@d.co", "disable:" + id);
	}
}

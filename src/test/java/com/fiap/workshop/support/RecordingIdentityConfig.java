package com.fiap.workshop.support;

import com.fiap.workshop.identity.application.port.out.IdentityProvider;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration(proxyBeanMethods = false)
public class RecordingIdentityConfig {

	public static class RecordingIdentityProvider implements IdentityProvider {

		public final List<CustomerAccount> provisioned = new CopyOnWriteArrayList<>();
		public final List<CustomerAccount> updated = new CopyOnWriteArrayList<>();
		public final List<UUID> disabled = new CopyOnWriteArrayList<>();

		@Override
		public void provisionCustomer(CustomerAccount account) {
			provisioned.add(account);
		}

		@Override
		public void updateCustomer(CustomerAccount account) {
			updated.add(account);
		}

		@Override
		public void disableCustomer(UUID customerId) {
			disabled.add(customerId);
		}
	}

	@Bean
	@Primary
	RecordingIdentityProvider recordingIdentityProvider() {
		return new RecordingIdentityProvider();
	}
}

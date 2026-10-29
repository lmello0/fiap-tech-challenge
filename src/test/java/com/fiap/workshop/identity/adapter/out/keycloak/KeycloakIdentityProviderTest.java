package com.fiap.workshop.identity.adapter.out.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.fiap.workshop.identity.application.port.out.IdentityProvider.CustomerAccount;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

class KeycloakIdentityProviderTest {

	private static final Network NETWORK = Network.newNetwork();
	private static GenericContainer<?> mailpit;
	private static KeycloakContainer keycloak;
	private static KeycloakIdentityProvider provider;

	@BeforeAll
	static void start() {
		mailpit = new GenericContainer<>("axllent/mailpit:v1.27")
				.withNetwork(NETWORK)
				.withNetworkAliases("mailpit")
				.withExposedPorts(8025)
				.waitingFor(Wait.forHttp("/readyz").forPort(8025));
		mailpit.start();
		keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.4")
				.withRealmImportFile("realm-workshop.json")
				.withNetwork(NETWORK);
		keycloak.start();
		provider = new KeycloakIdentityProvider(
				new KeycloakProperties(keycloak.getAuthServerUrl(), "workshop", "workshop-admin", "workshop-admin-secret"));
	}

	@AfterAll
	static void stop() {
		keycloak.stop();
		mailpit.stop();
	}

	private static RealmResource realm() {
		return keycloak.getKeycloakAdminClient().realm("workshop");
	}

	private static UserRepresentation userOf(UUID customerId) {
		List<UserRepresentation> users = realm().users().searchByAttributes("customer_id:" + customerId);
		assertThat(users).hasSize(1);
		return users.getFirst();
	}

	private static String mailpitMessages() {
		return RestClient.create("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025))
				.get().uri("/api/v1/messages").retrieve().body(String.class);
	}

	@Test
	void provisionsCustomerAccountWithRoleAttributeAndSetupEmail() {
		UUID customerId = UUID.randomUUID();

		provider.provisionCustomer(new CustomerAccount(customerId, "Maria Clara Santos", "maria.clara@example.com"));

		UserRepresentation user = userOf(customerId);
		assertThat(user.getUsername()).isEqualTo("maria.clara@example.com");
		assertThat(user.getEmail()).isEqualTo("maria.clara@example.com");
		assertThat(user.getFirstName()).isEqualTo("Maria");
		assertThat(user.getLastName()).isEqualTo("Clara Santos");
		assertThat(user.isEnabled()).isTrue();
		assertThat(user.getRequiredActions()).contains("UPDATE_PASSWORD", "VERIFY_EMAIL");
		assertThat(realm().users().get(user.getId()).roles().realmLevel().listAll())
				.extracting(RoleRepresentation::getName).contains("CUSTOMER");
		await().untilAsserted(() -> assertThat(mailpitMessages()).contains("maria.clara@example.com"));
	}

	@Test
	void provisioningTwiceDoesNotCreateASecondAccount() {
		UUID customerId = UUID.randomUUID();
		CustomerAccount account = new CustomerAccount(customerId, "Joao Silva", "joao.silva@example.com");

		provider.provisionCustomer(account);
		provider.provisionCustomer(account);

		assertThat(realm().users().searchByAttributes("customer_id:" + customerId)).hasSize(1);
	}

	@Test
	void updateChangesNameAndEmailWithoutReopeningPasswordSetup() {
		UUID customerId = UUID.randomUUID();
		provider.provisionCustomer(new CustomerAccount(customerId, "Pedro Alves", "pedro.alves@example.com"));
		var resource = realm().users().get(userOf(customerId).getId());
		UserRepresentation done = resource.toRepresentation();
		done.setRequiredActions(List.of());
		resource.update(done);

		provider.updateCustomer(new CustomerAccount(customerId, "Pedro Henrique Alves", "pedro.h@example.com"));

		UserRepresentation updated = userOf(customerId);
		assertThat(updated.getEmail()).isEqualTo("pedro.h@example.com");
		assertThat(updated.getUsername()).isEqualTo("pedro.h@example.com");
		assertThat(updated.getFirstName()).isEqualTo("Pedro");
		assertThat(updated.getLastName()).isEqualTo("Henrique Alves");
		assertThat(updated.getRequiredActions()).isEmpty();
		assertThat(updated.isEnabled()).isTrue();
	}

	@Test
	void disableTurnsTheAccountOffAndKeepsItsData() {
		UUID customerId = UUID.randomUUID();
		provider.provisionCustomer(new CustomerAccount(customerId, "Lia Costa", "lia.costa@example.com"));

		provider.disableCustomer(customerId);

		UserRepresentation user = userOf(customerId);
		assertThat(user.isEnabled()).isFalse();
		assertThat(user.getLastName()).isEqualTo("Costa");
	}

	@Test
	void updateOrDisableOfUnknownCustomerFailsSoTheEventIsRetried() {
		UUID unknown = UUID.randomUUID();

		assertThatThrownBy(() -> provider.updateCustomer(new CustomerAccount(unknown, "X Y", "x@y.co")))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> provider.disableCustomer(unknown)).isInstanceOf(IllegalStateException.class);
	}
}

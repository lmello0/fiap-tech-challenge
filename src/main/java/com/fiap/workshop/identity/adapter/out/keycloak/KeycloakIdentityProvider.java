package com.fiap.workshop.identity.adapter.out.keycloak;

import com.fiap.workshop.identity.application.port.out.IdentityProvider;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

@Component
class KeycloakIdentityProvider implements IdentityProvider {

	private static final String CUSTOMER_ROLE = "CUSTOMER";
	private static final List<String> SETUP_ACTIONS = List.of("UPDATE_PASSWORD", "VERIFY_EMAIL");
	private static final long SETUP_LINK_LIFESPAN_SECONDS = 172_800;
	private static final ParameterizedTypeReference<List<Map<String, Object>>> USERS = new ParameterizedTypeReference<>() {
	};

	private final KeycloakProperties properties;
	private final RestClient client;
	private volatile String cachedToken;
	private volatile Instant cachedTokenExpiry = Instant.EPOCH;

	KeycloakIdentityProvider(KeycloakProperties properties) {
		this.properties = properties;
		this.client = RestClient.builder().baseUrl(properties.baseUrl()).build();
	}

	@Override
	public void provisionCustomer(CustomerAccount account) {
		Map<String, Object> existing = findUser(account.customerId());
		String userId;
		if (existing == null) {
			userId = createUser(account);
			assignCustomerRole(userId);
		} else if (!pendingSetup(existing)) {
			return;
		} else {
			userId = (String) existing.get("id");
		}
		sendSetupEmail(userId);
	}

	@Override
	public void updateCustomer(CustomerAccount account) {
		Map<String, Object> user = requireUser(account.customerId());
		String[] names = splitName(account.name());
		put(user, Map.of("email", account.email(), "firstName", names[0], "lastName", names[1]));
	}

	@Override
	public void disableCustomer(UUID customerId) {
		Map<String, Object> user = requireUser(customerId);
		String id = (String) user.get("id");
		put(user, Map.of("enabled", false));
		client.post().uri(adminUri("/users/{id}/logout"), id).header("Authorization", bearer()).retrieve().toBodilessEntity();
	}

	private Map<String, Object> findUser(UUID customerId) {
		List<Map<String, Object>> users = client.get()
				.uri(adminUri("/users?q={q}&exact=true"), "customer_id:" + customerId)
				.header("Authorization", bearer())
				.retrieve()
				.body(USERS);
		return users == null || users.isEmpty() ? null : users.getFirst();
	}

	private Map<String, Object> requireUser(UUID customerId) {
		Map<String, Object> user = findUser(customerId);
		if (user == null) {
			throw new IllegalStateException("No Keycloak account linked to customer " + customerId);
		}
		return user;
	}

	private String createUser(CustomerAccount account) {
		var response = client.post()
				.uri(adminUri("/users"))
				.header("Authorization", bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.body(newUserRepresentation(account))
				.retrieve()
				.toBodilessEntity();
		String location = response.getHeaders().getFirst("Location");
		return location.substring(location.lastIndexOf('/') + 1);
	}

	private void put(Map<String, Object> user, Map<String, Object> changes) {
		Map<String, Object> body = new HashMap<>(user);
		body.remove("userProfileMetadata");
		body.putAll(changes);
		client.put()
				.uri(adminUri("/users/{id}"), user.get("id"))
				.header("Authorization", bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.body(body)
				.retrieve()
				.toBodilessEntity();
	}

	private void assignCustomerRole(String userId) {
		Map<String, Object> role = client.get()
				.uri(adminUri("/roles/{name}"), CUSTOMER_ROLE)
				.header("Authorization", bearer())
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		client.post()
				.uri(adminUri("/users/{id}/role-mappings/realm"), userId)
				.header("Authorization", bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.body(List.of(role))
				.retrieve()
				.toBodilessEntity();
	}

	private void sendSetupEmail(String userId) {
		client.put()
				.uri(adminUri("/users/{id}/execute-actions-email?lifespan={lifespan}"), userId, SETUP_LINK_LIFESPAN_SECONDS)
				.header("Authorization", bearer())
				.contentType(MediaType.APPLICATION_JSON)
				.body(SETUP_ACTIONS)
				.retrieve()
				.toBodilessEntity();
	}

	private static boolean pendingSetup(Map<String, Object> user) {
		return user.get("requiredActions") instanceof List<?> actions && actions.contains("UPDATE_PASSWORD");
	}

	private static Map<String, Object> newUserRepresentation(CustomerAccount account) {
		String[] names = splitName(account.name());
		return Map.of(
				"username", account.email(),
				"email", account.email(),
				"firstName", names[0],
				"lastName", names[1],
				"enabled", true,
				"requiredActions", SETUP_ACTIONS,
				"attributes", Map.of("customer_id", List.of(account.customerId().toString())));
	}

	private static String[] splitName(String name) {
		String trimmed = name.trim();
		int space = trimmed.indexOf(' ');
		return space < 0 ? new String[] {trimmed, ""} : new String[] {trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
	}

	private String adminUri(String path) {
		return "/admin/realms/" + properties.realm() + path;
	}

	private String bearer() {
		if (cachedToken == null || Instant.now().isAfter(cachedTokenExpiry)) {
			synchronized (this) {
				if (cachedToken == null || Instant.now().isAfter(cachedTokenExpiry)) {
					refreshToken();
				}
			}
		}
		return "Bearer " + cachedToken;
	}

	private void refreshToken() {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", "client_credentials");
		form.add("client_id", properties.adminClientId());
		form.add("client_secret", properties.adminClientSecret());
		Map<String, Object> response = client.post()
				.uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
		cachedToken = (String) response.get("access_token");
		long ttl = ((Number) response.get("expires_in")).longValue();
		cachedTokenExpiry = Instant.now().plusSeconds(Math.max(ttl - 30, 1));
	}
}

package com.fiap.workshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.support.RecordingIdentityConfig;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class AuthenticationE2ETest {

	@Container
	static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.4")
			.withRealmImportFile("realm-workshop.json")
			.withEnv("KEYCLOAK_ADMIN_CLIENT_SECRET", "workshop-admin-secret");

	@DynamicPropertySource
	static void keycloakProperties(DynamicPropertyRegistry registry) {
		String realm = keycloak.getAuthServerUrl() + "/realms/workshop";
		registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> realm);
		registry.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", () -> realm + "/protocol/openid-connect/certs");
	}

	@Autowired
	MockMvc mvc;

	private Map<String, Object> token(String grantType, Map<String, String> params) {
		var form = new LinkedMultiValueMap<String, String>();
		form.add("grant_type", grantType);
		form.add("client_id", "workshop-cli");
		params.forEach(form::add);
		return RestClient.create(keycloak.getAuthServerUrl()).post()
				.uri("/realms/workshop/protocol/openid-connect/token")
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(form)
				.retrieve()
				.body(new ParameterizedTypeReference<>() {
				});
	}

	private Map<String, Object> login(String user) {
		return token("password", Map.of("username", user, "password", user));
	}

	private static String bearer(Map<String, Object> tokens) {
		return "Bearer " + tokens.get("access_token");
	}

	@Test
	void realTokensCarryRolesThatDriveAuthorization() throws Exception {
		String attendant = bearer(login("attendant"));
		String mechanic = bearer(login("mechanic"));
		String customer = bearer(login("customer"));

		mvc.perform(get("/api/v1/customers").header("Authorization", attendant)).andExpect(status().isOk());
		mvc.perform(get("/api/v1/customers").header("Authorization", mechanic)).andExpect(status().isOk());
		mvc.perform(post("/api/v1/customers").header("Authorization", mechanic).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Ana\",\"document\":\"52998224725\",\"email\":\"ana@example.com\"}"))
				.andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/customers").header("Authorization", customer)).andExpect(status().isForbidden());
	}

	@Test
	void customerTokenCarriesTheLinkedCustomerId() throws Exception {
		mvc.perform(get("/api/v1/me").header("Authorization", bearer(login("customer")))).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/me").header("Authorization", bearer(login("attendant")))).andExpect(status().isForbidden());
	}

	@Test
	void refreshedAccessTokenIsAccepted() throws Exception {
		Map<String, Object> first = login("attendant");

		Map<String, Object> refreshed = token("refresh_token", Map.of("refresh_token", (String) first.get("refresh_token")));

		assertThat(refreshed.get("access_token")).isNotEqualTo(first.get("access_token"));
		mvc.perform(get("/api/v1/customers").header("Authorization", bearer(refreshed))).andExpect(status().isOk());
	}

	@Test
	void rejectsMissingOrTamperedTokens() throws Exception {
		String valid = bearer(login("admin"));

		mvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/customers").header("Authorization", valid + "x")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/customers").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
	}
}

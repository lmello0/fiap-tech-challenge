package com.fiap.workshop.customer.adapter.in.web;

import static com.fiap.workshop.support.TestDocuments.cpf;
import static com.fiap.workshop.support.TestDocuments.email;
import static com.fiap.workshop.support.TestDocuments.plate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.customer.api.events.CustomerRegistered;
import com.fiap.workshop.customer.api.events.VehicleRegistered;
import com.fiap.workshop.support.RecordingIdentityConfig;
import com.fiap.workshop.support.RecordingIdentityConfig.RecordingIdentityProvider;
import com.fiap.workshop.TestcontainersConfiguration;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class CustomerApiTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ApplicationEvents events;
	@Autowired
	RecordingIdentityProvider identity;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

	private static String customerJson(String name, String document, String email) {
		return """
				{"name":"%s","document":"%s","email":"%s","phone":"11999990000"}""".formatted(name, document, email);
	}

	private String createCustomer(String document, String email) throws Exception {
		String body = mvc.perform(post("/api/v1/customers").with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content(customerJson("Ana Souza", document, email)))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.id");
	}

	@Test
	void registersCustomerPublishesEventAndProvisionsAnAccount() throws Exception {
		String document = cpf();
		String email = email();

		mvc.perform(post("/api/v1/customers").with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content(customerJson("Ana Souza", document, email)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.document").value(document))
				.andExpect(jsonPath("$.documentType").value("CPF"))
				.andExpect(jsonPath("$.email").value(email))
				.andExpect(jsonPath("$.active").value(true));

		assertThat(events.stream(CustomerRegistered.class).filter(e -> e.email().equals(email))).hasSize(1);
		await().untilAsserted(() -> assertThat(identity.provisioned).anyMatch(a -> a.email().equals(email)));
	}

	@Test
	void rejectsDuplicateDocumentAndEmailWithConflict() throws Exception {
		String document = cpf();
		String email = email();
		createCustomer(document, email);

		mvc.perform(post("/api/v1/customers").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
						.content(customerJson("Other", document, email())))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409));
		mvc.perform(post("/api/v1/customers").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
						.content(customerJson("Other", cpf(), email)))
				.andExpect(status().isConflict());
	}

	@Test
	void validatesInput() throws Exception {
		mvc.perform(post("/api/v1/customers").with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content(customerJson("Ana", "12345678900", email())))
				.andExpect(status().isUnprocessableContent());
		mvc.perform(post("/api/v1/customers").with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content(customerJson("", cpf(), email())))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/customers").with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content(customerJson("Ana", cpf(), "not-an-email")))
				.andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/customers").with(as("ADMIN")).param("size", "500")).andExpect(status().isBadRequest());
	}

	@Test
	void enforcesRoles() throws Exception {
		mvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/customers").with(as("CUSTOMER"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/customers").with(as("MECHANIC"))).andExpect(status().isOk());
		mvc.perform(post("/api/v1/customers").with(as("MECHANIC")).contentType(MediaType.APPLICATION_JSON)
						.content(customerJson("Ana", cpf(), email())))
				.andExpect(status().isForbidden());
	}

	@Test
	void updatesFindsAndListsCustomers() throws Exception {
		String document = cpf();
		String id = createCustomer(document, email());
		String newEmail = email();

		mvc.perform(put("/api/v1/customers/{id}", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"name":"Ana Lima","email":"%s"}""".formatted(newEmail)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Ana Lima"));
		mvc.perform(get("/api/v1/customers/{id}", id).with(as("MECHANIC")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.email").value(newEmail));
		mvc.perform(get("/api/v1/customers").with(as("ADMIN")).param("document", document))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.content[0].id").value(id));
		mvc.perform(get("/api/v1/customers/{id}", UUID.randomUUID()).with(as("ADMIN"))).andExpect(status().isNotFound());
		await().untilAsserted(() -> assertThat(identity.updated).anyMatch(a -> a.email().equals(newEmail)));
	}

	@Test
	void deactivationHidesCustomerCascadesToVehiclesAndDisablesAccount() throws Exception {
		String id = createCustomer(cpf(), email());
		String vehicle = JsonPath.read(mvc.perform(post("/api/v1/customers/{id}/vehicles", id).with(as("ATTENDANT"))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"plate":"%s","brand":"Fiat","model":"Argo","year":2022}""".formatted(plate())))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");

		mvc.perform(delete("/api/v1/customers/{id}", id).with(as("ATTENDANT"))).andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/customers/{id}", id).with(as("ADMIN"))).andExpect(jsonPath("$.active").value(false));
		mvc.perform(get("/api/v1/customers").with(as("ADMIN")).param("name", "Ana Souza").param("size", "200"))
				.andExpect(jsonPath("$.content[?(@.id=='%s')]".formatted(id)).isEmpty());
		mvc.perform(get("/api/v1/customers").with(as("ADMIN")).param("includeInactive", "true").param("size", "200"))
				.andExpect(jsonPath("$.content[?(@.id=='%s')]".formatted(id)).isNotEmpty());
		mvc.perform(get("/api/v1/vehicles/{id}", vehicle).with(as("ADMIN"))).andExpect(jsonPath("$.active").value(false));
		mvc.perform(delete("/api/v1/customers/{id}", id).with(as("ATTENDANT"))).andExpect(status().isUnprocessableContent());
		await().untilAsserted(() -> assertThat(identity.disabled).contains(UUID.fromString(id)));
	}

	@Test
	void managesVehiclesOfACustomer() throws Exception {
		String customerId = createCustomer(cpf(), email());
		String plate = plate();
		String body = """
				{"plate":"%s","brand":"Fiat","model":"Argo","year":2022}""".formatted(plate);

		String vehicleId = JsonPath.read(mvc.perform(post("/api/v1/customers/{id}/vehicles", customerId).with(as("ATTENDANT"))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.plate").value(plate))
				.andExpect(jsonPath("$.customerId").value(customerId))
				.andReturn().getResponse().getContentAsString(), "$.id");
		assertThat(events.stream(VehicleRegistered.class).filter(e -> e.plate().equals(plate))).hasSize(1);

		mvc.perform(post("/api/v1/customers/{id}/vehicles", customerId).with(as("ATTENDANT"))
						.contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isConflict());
		mvc.perform(post("/api/v1/customers/{id}/vehicles", UUID.randomUUID()).with(as("ATTENDANT"))
						.contentType(MediaType.APPLICATION_JSON).content(body.replace(plate, plate()))).andExpect(status().isNotFound());
		mvc.perform(post("/api/v1/customers/{id}/vehicles", customerId).with(as("ATTENDANT"))
						.contentType(MediaType.APPLICATION_JSON).content(body.replace(plate, "12")))
				.andExpect(status().isUnprocessableContent());

		mvc.perform(put("/api/v1/vehicles/{id}", vehicleId).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand":"VW","model":"Polo","year":2023}"""))
				.andExpect(status().isOk()).andExpect(jsonPath("$.model").value("Polo"));
		mvc.perform(get("/api/v1/customers/{id}/vehicles", customerId).with(as("MECHANIC")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/vehicles").with(as("ADMIN"))).andExpect(status().isOk());

		mvc.perform(delete("/api/v1/vehicles/{id}", vehicleId).with(as("ATTENDANT"))).andExpect(status().isNoContent());
		mvc.perform(get("/api/v1/customers/{id}/vehicles", customerId).with(as("MECHANIC")))
				.andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(put("/api/v1/vehicles/{id}", vehicleId).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand":"VW","model":"Polo","year":2023}"""))
				.andExpect(status().isUnprocessableContent());
	}

	@Test
	void customerReadsOnlyTheirOwnProfile() throws Exception {
		String id = createCustomer(cpf(), email());

		mvc.perform(get("/api/v1/me").with(jwt()
						.authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")).jwt(j -> j.claim("customer_id", id))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
		mvc.perform(get("/api/v1/me").with(as("CUSTOMER"))).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/me").with(as("ATTENDANT"))).andExpect(status().isForbidden());
	}
}

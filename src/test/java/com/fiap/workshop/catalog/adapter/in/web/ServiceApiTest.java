package com.fiap.workshop.catalog.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.catalog.api.CatalogQueries;
import com.fiap.workshop.catalog.api.events.ServiceCreated;
import com.fiap.workshop.support.RecordingIdentityConfig;
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
class ServiceApiTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ApplicationEvents events;
	@Autowired
	CatalogQueries queries;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

	private static String body(String name, String price, int minutes) {
		return """
				{"name":"%s","description":"desc","price":%s,"estimatedMinutes":%d}""".formatted(name, price, minutes);
	}

	private String create(String name) throws Exception {
		return JsonPath.read(mvc.perform(post("/api/v1/services").with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content(body(name, "150.50", 45)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
	}

	private static String unique(String prefix) {
		return prefix + " " + UUID.randomUUID();
	}

	@Test
	void createsServicePublishesEventAndExposesItToOtherModules() throws Exception {
		String name = unique("Brake pads");

		String id = JsonPath.read(mvc.perform(post("/api/v1/services").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
						.content(body(name, "150.5", 45)))
				.andExpect(status().isCreated())
				.andExpect(header().exists("Location"))
				.andExpect(jsonPath("$.name").value(name))
				.andExpect(jsonPath("$.price").value(150.5))
				.andExpect(jsonPath("$.active").value(true))
				.andReturn().getResponse().getContentAsString(), "$.id");

		assertThat(events.stream(ServiceCreated.class).filter(e -> e.name().equals(name))).hasSize(1);
		assertThat(queries.findServiceById(UUID.fromString(id))).get().extracting("name").isEqualTo(name);
	}

	@Test
	void rejectsDuplicateNameAndInvalidInput() throws Exception {
		String name = unique("Alignment");
		create(name);

		mvc.perform(post("/api/v1/services").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body(name.toUpperCase(), "10", 30)))
				.andExpect(status().isConflict());
		mvc.perform(post("/api/v1/services").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body("", "10", 30)))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/services").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body("X", "-5", 30)))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/services").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body("X", "10", 0)))
				.andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/services").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(body("X", "10.123", 5)))
				.andExpect(status().isBadRequest());
	}

	@Test
	void enforcesRoles() throws Exception {
		mvc.perform(get("/api/v1/services")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/services").with(as("CUSTOMER"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/services").with(as("MECHANIC"))).andExpect(status().isOk());
		mvc.perform(post("/api/v1/services").with(as("MECHANIC")).contentType(MediaType.APPLICATION_JSON).content(body(unique("X"), "10", 30)))
				.andExpect(status().isForbidden());
	}

	@Test
	void updatesFindsAndListsServices() throws Exception {
		String name = unique("Wheel balancing");
		String id = create(name);

		mvc.perform(put("/api/v1/services/{id}", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content(body(name, "99.9", 20)))
				.andExpect(status().isOk()).andExpect(jsonPath("$.price").value(99.9)).andExpect(jsonPath("$.estimatedMinutes").value(20));
		mvc.perform(get("/api/v1/services/{id}", id).with(as("MECHANIC"))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value(name));
		mvc.perform(get("/api/v1/services").with(as("MECHANIC")).param("name", name.toLowerCase()))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/services/{id}", UUID.randomUUID()).with(as("ADMIN"))).andExpect(status().isNotFound());
		mvc.perform(put("/api/v1/services/{id}", UUID.randomUUID()).with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
						.content(body("Y", "1", 1)))
				.andExpect(status().isNotFound());
	}

	@Test
	void deactivatedServiceIsHiddenUnlessRequested() throws Exception {
		String name = unique("Coolant flush");
		String id = create(name);

		mvc.perform(delete("/api/v1/services/{id}", id).with(as("ATTENDANT"))).andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/services").with(as("ADMIN")).param("name", name)).andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/v1/services").with(as("ADMIN")).param("name", name).param("includeInactive", "true"))
				.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/services/{id}", id).with(as("ADMIN"))).andExpect(jsonPath("$.active").value(false));
		mvc.perform(delete("/api/v1/services/{id}", id).with(as("ATTENDANT"))).andExpect(status().isUnprocessableContent());
		assertThat(queries.findServiceById(UUID.fromString(id))).get().extracting("active").isEqualTo(false);
	}
}

package com.fiap.workshop.inventory.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.inventory.api.InventoryQueries;
import com.fiap.workshop.inventory.api.events.LowStockDetected;
import com.fiap.workshop.support.RecordingIdentityConfig;
import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
class PartApiTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ApplicationEvents events;
	@Autowired
	InventoryQueries queries;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)).jwt(j -> j.subject("user-" + role));
	}

	private static String sku() {
		return ("SKU-" + UUID.randomUUID().toString().substring(0, 12)).toUpperCase();
	}

	private static String partJson(String sku, int minimum) {
		return """
				{"sku":"%s","name":"Oil filter","description":"OEM","unitPrice":35.5,"minimumQuantity":%d}""".formatted(sku, minimum);
	}

	private String createPart(int minimum) throws Exception {
		return JsonPath.read(mvc.perform(post("/api/v1/parts").with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content(partJson(sku(), minimum)))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
	}

	private void entry(String id, int quantity) throws Exception {
		mvc.perform(post("/api/v1/parts/{id}/stock-entries", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":%d,\"note\":\"invoice\"}".formatted(quantity))).andExpect(status().isCreated());
	}

	@Test
	void createsPartWithZeroStockAndExposesItToOtherModules() throws Exception {
		String sku = sku();

		String id = JsonPath.read(mvc.perform(post("/api/v1/parts").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
						.content(partJson(sku.toLowerCase(), 5)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.sku").value(sku))
				.andExpect(jsonPath("$.onHand").value(0))
				.andExpect(jsonPath("$.lowStock").value(true))
				.andReturn().getResponse().getContentAsString(), "$.id");

		assertThat(queries.findPartById(UUID.fromString(id))).get().extracting("sku").isEqualTo(sku);
		mvc.perform(post("/api/v1/parts").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON).content(partJson(sku, 5)))
				.andExpect(status().isConflict());
	}

	@Test
	void validatesInputAndRoles() throws Exception {
		mvc.perform(post("/api/v1/parts").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"sku\":\"\",\"name\":\"A\",\"unitPrice\":1,\"minimumQuantity\":0}")).andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/parts").with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"sku\":\"S\",\"name\":\"A\",\"unitPrice\":-1,\"minimumQuantity\":0}")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/parts")).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/parts").with(as("CUSTOMER"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/parts").with(as("MECHANIC"))).andExpect(status().isOk());
		mvc.perform(post("/api/v1/parts").with(as("MECHANIC")).contentType(MediaType.APPLICATION_JSON).content(partJson(sku(), 1)))
				.andExpect(status().isForbidden());
	}

	@Test
	void stockEntriesAndAdjustmentsBuildAnAuditTrail() throws Exception {
		String id = createPart(5);
		entry(id, 10);

		mvc.perform(post("/api/v1/parts/{id}/stock-adjustments", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"delta\":-6,\"reason\":\"damaged\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.type").value("ADJUSTMENT"))
				.andExpect(jsonPath("$.balanceAfter").value(4))
				.andExpect(jsonPath("$.performedBy").value("user-ATTENDANT"));

		mvc.perform(get("/api/v1/parts/{id}", id).with(as("MECHANIC")))
				.andExpect(jsonPath("$.onHand").value(4)).andExpect(jsonPath("$.lowStock").value(true));
		mvc.perform(get("/api/v1/parts/{id}/movements", id).with(as("MECHANIC")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalElements").value(2))
				.andExpect(jsonPath("$.content[0].type").value("ADJUSTMENT"))
				.andExpect(jsonPath("$.content[1].type").value("ENTRY"));
		assertThat(events.stream(LowStockDetected.class).filter(e -> e.partId().toString().equals(id))).hasSize(1);
	}

	@Test
	void rejectsInvalidStockOperations() throws Exception {
		String id = createPart(0);
		entry(id, 2);

		mvc.perform(post("/api/v1/parts/{id}/stock-adjustments", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"delta\":-3,\"reason\":\"oops\"}")).andExpect(status().isUnprocessableContent());
		mvc.perform(post("/api/v1/parts/{id}/stock-adjustments", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"delta\":1}")).andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/parts/{id}/stock-entries", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":0}")).andExpect(status().isBadRequest());
		mvc.perform(post("/api/v1/parts/{id}/stock-entries", UUID.randomUUID()).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":1}")).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/parts/{id}", id).with(as("ADMIN"))).andExpect(jsonPath("$.onHand").value(2));
	}

	@Test
	void updatesFiltersAndDeactivates() throws Exception {
		String id = createPart(5);

		mvc.perform(put("/api/v1/parts/{id}", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
						.content("{\"name\":\"Oil filter XL\",\"unitPrice\":40,\"minimumQuantity\":2}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Oil filter XL")).andExpect(jsonPath("$.minimumQuantity").value(2));
		mvc.perform(get("/api/v1/parts").with(as("ADMIN")).param("text", "filter xl").param("lowStockOnly", "true").param("size", "200"))
				.andExpect(jsonPath("$.content[?(@.id=='%s')]".formatted(id)).isNotEmpty());

		mvc.perform(delete("/api/v1/parts/{id}", id).with(as("ATTENDANT"))).andExpect(status().isNoContent());

		mvc.perform(get("/api/v1/parts").with(as("ADMIN")).param("text", "filter xl").param("size", "200"))
				.andExpect(jsonPath("$.content[?(@.id=='%s')]".formatted(id)).isEmpty());
		mvc.perform(post("/api/v1/parts/{id}/stock-entries", id).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"quantity\":1}")).andExpect(status().isUnprocessableContent());
	}

	@Test
	void concurrentStockEntriesNeverLoseAnUpdate() throws Exception {
		String id = createPart(0);
		int attempts = 12;
		ExecutorService pool = Executors.newFixedThreadPool(attempts);
		List<Future<Integer>> results = new ArrayList<>();
		for (int i = 0; i < attempts; i++) {
			results.add(pool.submit(() -> mvc.perform(post("/api/v1/parts/{id}/stock-entries", id).with(as("ATTENDANT"))
							.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":1}"))
					.andReturn().getResponse().getStatus()));
		}
		int created = 0;
		for (Future<Integer> result : results) {
			int code = result.get();
			assertThat(code).isIn(201, 409);
			if (code == 201) {
				created++;
			}
		}
		pool.shutdown();

		assertThat(created).isPositive();
		mvc.perform(get("/api/v1/parts/{id}", id).with(as("ADMIN"))).andExpect(jsonPath("$.onHand").value(created));
		mvc.perform(get("/api/v1/parts/{id}/movements", id).with(as("ADMIN"))).andExpect(jsonPath("$.totalElements").value(created));
	}
}

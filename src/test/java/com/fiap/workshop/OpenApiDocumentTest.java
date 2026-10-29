package com.fiap.workshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.support.RecordingIdentityConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class OpenApiDocumentTest {

	private static final Path DOCUMENT = Path.of("docs/api/openapi.json");

	@Autowired
	MockMvc mvc;
	@Autowired
	JsonMapper json;

	@Test
	void theVersionedOpenApiDocumentMatchesTheRunningApi() throws Exception {
		String generated = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		JsonNode current = json.readTree(generated);

		if (Boolean.getBoolean("openapi.update")) {
			Files.createDirectories(DOCUMENT.getParent());
			Files.writeString(DOCUMENT, json.writerWithDefaultPrettyPrinter().writeValueAsString(current) + "\n");
		}

		assertThat(DOCUMENT).as("run ./mvnw test -Dtest=OpenApiDocumentTest -Dopenapi.update=true to regenerate it").exists();
		assertThat(json.readTree(Files.readString(DOCUMENT))).isEqualTo(current);
	}

	@Test
	void documentsTheMainOperations() throws Exception {
		String generated = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
		JsonNode paths = json.readTree(generated).get("paths");

		assertThat(paths.has("/api/v1/work-orders")).isTrue();
		assertThat(paths.has("/api/v1/work-orders/metrics")).isTrue();
		assertThat(paths.has("/api/v1/webhooks/budget-decisions")).isTrue();
		assertThat(paths.has("/api/v1/me/work-orders/{id}/budgets/{budgetId}/approve")).isTrue();
		assertThat(paths.has("/api/v1/parts/{id}/stock-entries")).isTrue();
	}
}

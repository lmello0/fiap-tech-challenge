package com.fiap.workshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.support.RecordingIdentityConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
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

	// springdoc names schemas after the simple class name, so two records called Create silently share one schema.
	@Test
	void everyRequestBodyIsDescribedByItsOwnSchema() throws Exception {
		JsonNode doc = generated();

		assertThat(bodyFields(doc, "/api/v1/customers", "post")).containsExactlyInAnyOrder("name", "document", "email", "phone");
		assertThat(bodyFields(doc, "/api/v1/customers/{id}", "put")).containsExactlyInAnyOrder("name", "email", "phone");
		assertThat(bodyFields(doc, "/api/v1/customers/{customerId}/vehicles", "post"))
				.containsExactlyInAnyOrder("plate", "brand", "model", "year");
		assertThat(bodyFields(doc, "/api/v1/vehicles/{id}", "put")).containsExactlyInAnyOrder("brand", "model", "year");
		assertThat(bodyFields(doc, "/api/v1/parts", "post"))
				.containsExactlyInAnyOrder("sku", "name", "description", "unitPrice", "minimumQuantity");
		assertThat(bodyFields(doc, "/api/v1/parts/{id}", "put"))
				.containsExactlyInAnyOrder("name", "description", "unitPrice", "minimumQuantity");
		assertThat(bodyFields(doc, "/api/v1/webhooks/budget-decisions", "post"))
				.containsExactlyInAnyOrder("eventId", "workOrderNumber", "budgetId", "decision", "reason");

		JsonNode open = schema(doc, doc.at("/paths/~1api~1v1~1work-orders/post/requestBody/content/application~1json/schema"));
		assertThat(fields(schema(doc, open.at("/properties/customer")))).containsExactlyInAnyOrder("document", "name", "email", "phone");
		assertThat(fields(schema(doc, open.at("/properties/vehicle")))).containsExactlyInAnyOrder("plate", "brand", "model", "year");
	}

	@Test
	void documentsTheStatusCodesTheApiReturns() throws Exception {
		JsonNode paths = generated().get("paths");

		assertThat(paths.at("/~1api~1v1~1customers/post/responses").has("201")).isTrue();
		assertThat(paths.at("/~1api~1v1~1customers/post/responses").has("200")).isFalse();
		assertThat(paths.at("/~1api~1v1~1parts~1{id}~1stock-entries/post/responses").has("201")).isTrue();
		assertThat(paths.at("/~1api~1v1~1work-orders/post/responses").has("201")).isTrue();
		assertThat(paths.at("/~1api~1v1~1customers~1{id}/delete/responses").has("204")).isTrue();
		assertThat(paths.at("/~1api~1v1~1customers/post/responses/409/content").has("application/problem+json")).isTrue();
		assertThat(paths.at("/~1api~1v1~1work-orders~1{id}/get/responses").has("404")).isTrue();
	}

	@Test
	void theWebhookIsPublicAndStaffOperationsListTheirRoles() throws Exception {
		JsonNode paths = generated().get("paths");

		JsonNode webhook = paths.at("/~1api~1v1~1webhooks~1budget-decisions/post");
		assertThat(webhook.get("security")).isEmpty();
		assertThat(webhook.get("responses").has("403")).isFalse();
		assertThat(paths.at("/~1api~1v1~1services/post/description").asString()).contains("ADMIN or ATTENDANT");
		assertThat(paths.at("/~1api~1v1~1me~1work-orders/get/description").asString()).contains("CUSTOMER");
	}

	private JsonNode generated() throws Exception {
		return json.readTree(mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString());
	}

	private static List<String> bodyFields(JsonNode doc, String path, String method) {
		return fields(schema(doc, doc.get("paths").get(path).get(method).at("/requestBody/content/application~1json/schema")));
	}

	private static JsonNode schema(JsonNode doc, JsonNode node) {
		JsonNode ref = node.get("$ref");
		return ref == null ? node : doc.at(ref.asString().substring(1));
	}

	private static List<String> fields(JsonNode schema) {
		return schema.get("properties").properties().stream().map(Map.Entry::getKey).toList();
	}
}

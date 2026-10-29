package com.fiap.workshop.notification;

import static com.fiap.workshop.support.TestDocuments.cpf;
import static com.fiap.workshop.support.TestDocuments.plate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.support.RecordingIdentityConfig;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class NotificationFlowTest {

	@Container
	static GenericContainer<?> mailpit = new GenericContainer<>("axllent/mailpit:v1.27")
			.withExposedPorts(1025, 8025)
			.waitingFor(Wait.forHttp("/readyz").forPort(8025));

	@DynamicPropertySource
	static void smtp(DynamicPropertyRegistry registry) {
		registry.add("spring.mail.host", mailpit::getHost);
		registry.add("spring.mail.port", () -> mailpit.getMappedPort(1025));
	}

	@Autowired
	MockMvc mvc;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)).jwt(j -> j.subject("user-" + role));
	}

	private String send(String url, String body, String role) throws Exception {
		return mvc.perform(post(url).with(as(role)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
	}

	private String inbox() {
		return RestClient.create("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025)).get().uri("/api/v1/messages?limit=200")
				.retrieve().body(String.class);
	}

	private String message(String id) {
		return RestClient.create("http://" + mailpit.getHost() + ":" + mailpit.getMappedPort(8025)).get().uri("/api/v1/message/" + id)
				.retrieve().body(String.class);
	}

	@Test
	void budgetCompletionAndLowStockEmailsReachTheirRecipients() throws Exception {
		String email = "flow-" + UUID.randomUUID() + "@example.com";
		String document = cpf();
		String plate = plate();
		String customerId = JsonPath.read(send("/api/v1/customers", "{\"name\":\"Ana Souza\",\"document\":\"%s\",\"email\":\"%s\"}".formatted(document, email), "ATTENDANT"), "$.id");
		send("/api/v1/customers/%s/vehicles".formatted(customerId), "{\"plate\":\"%s\",\"brand\":\"Fiat\",\"model\":\"Argo\",\"year\":2022}".formatted(plate), "ATTENDANT");
		String service = JsonPath.read(send("/api/v1/services", "{\"name\":\"Service %s\",\"price\":150,\"estimatedMinutes\":30}".formatted(UUID.randomUUID()), "ADMIN"), "$.id");
		String part = JsonPath.read(send("/api/v1/parts", "{\"sku\":\"SKU-%s\",\"name\":\"Flow filter\",\"unitPrice\":35,\"minimumQuantity\":5}"
				.formatted(UUID.randomUUID().toString().substring(0, 12)), "ADMIN"), "$.id");
		send("/api/v1/parts/%s/stock-entries".formatted(part), "{\"quantity\":8}", "ATTENDANT");

		String workOrder = JsonPath.read(send("/api/v1/work-orders", "{\"customer\":{\"document\":\"%s\",\"name\":\"Ana Souza\",\"email\":\"ana@example.com\"},\"vehicle\":{\"plate\":\"%s\",\"brand\":\"Fiat\",\"model\":\"Argo\",\"year\":2022},\"items\":[{\"type\":\"SERVICE\",\"referenceId\":\"%s\",\"quantity\":1},{\"type\":\"PART\",\"referenceId\":\"%s\",\"quantity\":4}]}"
				.formatted(document, plate, service, part), "ATTENDANT"), "$.id");
		send("/api/v1/work-orders/%s/diagnosis/start".formatted(workOrder), "", "MECHANIC");
		String body = send("/api/v1/work-orders/%s/diagnosis/complete".formatted(workOrder), "", "MECHANIC");
		String number = JsonPath.read(body, "$.number");
		List<String> budgets = JsonPath.read(body, "$.budgets[*].id");

		await().untilAsserted(() -> assertThat(inbox()).contains(email).contains("awaits your approval").contains(number));
		List<String> ids = JsonPath.read(inbox(), "$.messages[?(@.To[0].Address=='%s')].ID".formatted(email));
		String budgetId = ids.getFirst();
		assertThat(message(budgetId)).contains("Flow filter").contains("BRL 290.00").contains("Ana Souza");

		mvc.perform(post("/api/v1/me/work-orders/{id}/budgets/{budget}/approve", workOrder, budgets.getLast())
				.with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")).jwt(j -> j.claim("customer_id", customerId))))
				.andExpect(status().isOk());

		await().untilAsserted(() -> assertThat(inbox()).contains("Low stock: Flow filter"));
		await().untilAsserted(() -> assertThat(inbox()).contains("admin@workshop.local"));

		await().untilAsserted(() -> mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/work-orders/{id}", workOrder)
				.with(as("ADMIN"))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.status").value("IN_EXECUTION")));
		send("/api/v1/work-orders/%s/complete".formatted(workOrder), "", "MECHANIC");

		await().untilAsserted(() -> assertThat(inbox()).contains("Your vehicle is ready: work order " + number));
	}
}

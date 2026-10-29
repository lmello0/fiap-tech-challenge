package com.fiap.workshop.stock;

import static com.fiap.workshop.support.TestDocuments.cpf;
import static com.fiap.workshop.support.TestDocuments.email;
import static com.fiap.workshop.support.TestDocuments.plate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.inventory.api.events.LowStockDetected;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class StockSagaTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	LowStockCapture lowStockEvents;

	static class LowStockCapture {

		final List<LowStockDetected> received = new java.util.concurrent.CopyOnWriteArrayList<>();

		@org.springframework.context.event.EventListener
		void on(LowStockDetected event) {
			received.add(event);
		}
	}

	@org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
	static class Capture {

		@org.springframework.context.annotation.Bean
		LowStockCapture lowStockCapture() {
			return new LowStockCapture();
		}
	}

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)).jwt(j -> j.subject("user-" + role));
	}

	private static RequestPostProcessor asCustomer(String customerId) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")).jwt(j -> j.claim("customer_id", customerId));
	}

	private String send(String url, String body, String role) throws Exception {
		return mvc.perform(post(url).with(as(role)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
	}

	private record Customer(String id, String document, String plate) {
	}

	private Customer customer() throws Exception {
		String document = cpf();
		String id = JsonPath.read(send("/api/v1/customers",
				"{\"name\":\"Ana\",\"document\":\"%s\",\"email\":\"%s\"}".formatted(document, email()), "ATTENDANT"), "$.id");
		String plate = plate();
		send("/api/v1/customers/%s/vehicles".formatted(id), "{\"plate\":\"%s\",\"brand\":\"Fiat\",\"model\":\"Argo\",\"year\":2022}".formatted(plate), "ATTENDANT");
		return new Customer(id, document, plate);
	}

	private String service() throws Exception {
		return JsonPath.read(send("/api/v1/services", "{\"name\":\"Service %s\",\"price\":100,\"estimatedMinutes\":30}".formatted(UUID.randomUUID()), "ADMIN"), "$.id");
	}

	private String part(int minimum, int stock) throws Exception {
		String id = JsonPath.read(send("/api/v1/parts", "{\"sku\":\"SKU-%s\",\"name\":\"Part\",\"unitPrice\":20,\"minimumQuantity\":%d}"
				.formatted(UUID.randomUUID().toString().substring(0, 12), minimum), "ADMIN"), "$.id");
		if (stock > 0) {
			send("/api/v1/parts/%s/stock-entries".formatted(id), "{\"quantity\":%d}".formatted(stock), "ATTENDANT");
		}
		return id;
	}

	private String openWithPart(Customer c, String serviceId, String partId, int quantity) throws Exception {
		String id = JsonPath.read(send("/api/v1/work-orders", "{\"customer\":{\"document\":\"%s\",\"name\":\"Ana Souza\",\"email\":\"ana@example.com\"},\"vehicle\":{\"plate\":\"%s\",\"brand\":\"Fiat\",\"model\":\"Argo\",\"year\":2022},\"items\":[{\"type\":\"SERVICE\",\"referenceId\":\"%s\",\"quantity\":1},{\"type\":\"PART\",\"referenceId\":\"%s\",\"quantity\":%d}]}"
				.formatted(c.document(), c.plate(), serviceId, partId, quantity), "ATTENDANT"), "$.id");
		send("/api/v1/work-orders/%s/diagnosis/start".formatted(id), "", "MECHANIC");
		return id;
	}

	private String approve(Customer c, String workOrderId) throws Exception {
		String body = send("/api/v1/work-orders/%s/diagnosis/complete".formatted(workOrderId), "", "MECHANIC");
		List<String> budgets = JsonPath.read(body, "$.budgets[*].id");
		String budgetId = budgets.getLast();
		mvc.perform(post("/api/v1/me/work-orders/{id}/budgets/{budget}/approve", workOrderId, budgetId).with(asCustomer(c.id())))
				.andExpect(status().isOk());
		return budgetId;
	}

	private ResultActions workOrder(String id) throws Exception {
		return mvc.perform(get("/api/v1/work-orders/{id}", id).with(as("ADMIN")));
	}

	private void awaitStatus(String workOrderId, String expected) {
		await().untilAsserted(() -> workOrder(workOrderId).andExpect(jsonPath("$.status").value(expected)));
	}

	private ResultActions part(String id) throws Exception {
		return mvc.perform(get("/api/v1/parts/{id}", id).with(as("ADMIN")));
	}

	@Test
	void approvalReservesStockAndStartsExecutionThenCompletionConsumesIt() throws Exception {
		Customer c = customer();
		String partId = part(0, 10);
		String id = openWithPart(c, service(), partId, 4);

		approve(c, id);

		awaitStatus(id, "IN_EXECUTION");
		part(partId).andExpect(jsonPath("$.onHand").value(10)).andExpect(jsonPath("$.reserved").value(4)).andExpect(jsonPath("$.available").value(6));
		workOrder(id).andExpect(jsonPath("$.history[?(@.to=='IN_EXECUTION')].by.role").value("SYSTEM"));

		send("/api/v1/work-orders/%s/complete".formatted(id), "", "MECHANIC");

		await().untilAsserted(() -> part(partId).andExpect(jsonPath("$.onHand").value(6)).andExpect(jsonPath("$.reserved").value(0)));
		mvc.perform(get("/api/v1/parts/{id}/movements", partId).with(as("ADMIN")))
				.andExpect(jsonPath("$.content[0].type").value("CONSUMPTION"))
				.andExpect(jsonPath("$.content[0].quantity").value(-4))
				.andExpect(jsonPath("$.content[0].balanceAfter").value(6));
	}

	@Test
	void aShortagePausesTheOrderUntilAStockEntryArrives() throws Exception {
		Customer c = customer();
		String partId = part(0, 1);
		String id = openWithPart(c, service(), partId, 3);

		approve(c, id);

		awaitStatus(id, "WAITING_PARTS");
		part(partId).andExpect(jsonPath("$.reserved").value(0)).andExpect(jsonPath("$.onHand").value(1));

		send("/api/v1/parts/%s/stock-entries".formatted(partId), "{\"quantity\":5,\"note\":\"invoice\"}", "ATTENDANT");

		awaitStatus(id, "IN_EXECUTION");
		part(partId).andExpect(jsonPath("$.reserved").value(3)).andExpect(jsonPath("$.available").value(3));
	}

	@Test
	void cancellingReleasesTheReservationAndHealsTheNextOrderInLine() throws Exception {
		String partId = part(0, 4);
		Customer holder = customer();
		Customer queued = customer();
		String service = service();
		String holderId = openWithPart(holder, service, partId, 4);
		approve(holder, holderId);
		awaitStatus(holderId, "IN_EXECUTION");

		String queuedId = openWithPart(queued, service, partId, 3);
		approve(queued, queuedId);
		awaitStatus(queuedId, "WAITING_PARTS");

		send("/api/v1/work-orders/%s/cancel".formatted(holderId), "{\"reason\":\"customer gave up\"}", "ATTENDANT");

		awaitStatus(queuedId, "IN_EXECUTION");
		part(partId).andExpect(jsonPath("$.reserved").value(3)).andExpect(jsonPath("$.onHand").value(4));
	}

	@Test
	void anAdditionalBudgetWithPartsReservesAgainAndKeepsEarlierReservations() throws Exception {
		Customer c = customer();
		String partId = part(0, 10);
		String id = openWithPart(c, service(), partId, 2);
		approve(c, id);
		awaitStatus(id, "IN_EXECUTION");

		String body = send("/api/v1/work-orders/%s/additional-budget".formatted(id),
				"{\"items\":[{\"type\":\"PART\",\"referenceId\":\"%s\",\"quantity\":3}]}".formatted(partId), "MECHANIC");
		List<String> budgets = JsonPath.read(body, "$.budgets[*].id");
		mvc.perform(post("/api/v1/me/work-orders/{id}/budgets/{budget}/approve", id, budgets.getLast()).with(asCustomer(c.id())))
				.andExpect(status().isOk());

		awaitStatus(id, "IN_EXECUTION");
		part(partId).andExpect(jsonPath("$.reserved").value(5));

		send("/api/v1/work-orders/%s/complete".formatted(id), "", "MECHANIC");
		await().untilAsserted(() -> part(partId).andExpect(jsonPath("$.onHand").value(5)).andExpect(jsonPath("$.reserved").value(0)));
	}

	@Test
	void reservingBelowTheMinimumRaisesALowStockAlert() throws Exception {
		Customer c = customer();
		String partId = part(5, 8);
		String id = openWithPart(c, service(), partId, 4);

		approve(c, id);

		awaitStatus(id, "IN_EXECUTION");
		await().untilAsserted(() -> assertThat(lowStockEvents.received.stream().filter(e -> e.partId().toString().equals(partId)))
				.singleElement().satisfies(e -> assertThat(e.available()).isEqualTo(4)));
		part(partId).andExpect(jsonPath("$.lowStock").value(true));
	}

	@Test
	void concurrentApprovalsNeverReserveMoreThanTheAvailableStock() throws Exception {
		String partId = part(0, 5);
		String service = service();
		List<String> orders = new java.util.ArrayList<>();
		for (int i = 0; i < 4; i++) {
			Customer c = customer();
			String id = openWithPart(c, service, partId, 2);
			orders.add(id);
			approve(c, id);
		}

		await().untilAsserted(() -> {
			long executing = orders.stream().filter(this::isExecuting).count();
			long waiting = orders.stream().filter(this::isWaitingForParts).count();
			assertThat(executing).isEqualTo(2);
			assertThat(waiting).isEqualTo(2);
		});
		part(partId).andExpect(jsonPath("$.reserved").value(4)).andExpect(jsonPath("$.available").value(1));
	}

	private boolean isExecuting(String id) {
		return statusOf(id).equals("IN_EXECUTION");
	}

	private boolean isWaitingForParts(String id) {
		return statusOf(id).equals("WAITING_PARTS");
	}

	private String statusOf(String id) {
		try {
			return JsonPath.read(workOrder(id).andReturn().getResponse().getContentAsString(), "$.status");
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}

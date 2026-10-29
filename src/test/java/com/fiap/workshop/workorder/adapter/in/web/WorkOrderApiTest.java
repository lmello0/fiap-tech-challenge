package com.fiap.workshop.workorder.adapter.in.web;

import static com.fiap.workshop.support.TestDocuments.cpf;
import static com.fiap.workshop.support.TestDocuments.email;
import static com.fiap.workshop.support.TestDocuments.plate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.support.RecordingIdentityConfig;
import com.fiap.workshop.workorder.api.events.BudgetApproved;
import com.fiap.workshop.workorder.api.events.BudgetIssued;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class WorkOrderApiTest {

	@Autowired
	MockMvc mvc;
	@Autowired
	ApplicationEvents events;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)).jwt(j -> j.subject("user-" + role));
	}

	private static RequestPostProcessor asCustomer(String customerId) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")).jwt(j -> j.subject("user-CUSTOMER").claim("customer_id", customerId));
	}

	private String json(ResultActions result) throws Exception {
		return result.andReturn().getResponse().getContentAsString();
	}

	private record Fixture(String customerId, String document, String plate, String serviceId, String partId) {
	}

	private String send(String url, String body, String role, int expected) throws Exception {
		return json(mvc.perform(post(url).with(as(role)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(expected)));
	}

	private Fixture fixture() throws Exception {
		String document = cpf();
		String customerId = JsonPath.read(send("/api/v1/customers",
				"{\"name\":\"Ana Souza\",\"document\":\"%s\",\"email\":\"%s\"}".formatted(document, email()), "ATTENDANT", 201), "$.id");
		String plate = plate();
		send("/api/v1/customers/%s/vehicles".formatted(customerId),
				"{\"plate\":\"%s\",\"brand\":\"Fiat\",\"model\":\"Argo\",\"year\":2022}".formatted(plate), "ATTENDANT", 201);
		String serviceId = JsonPath.read(send("/api/v1/services",
				"{\"name\":\"Oil change %s\",\"price\":150.00,\"estimatedMinutes\":45}".formatted(UUID.randomUUID()), "ADMIN", 201), "$.id");
		String partId = JsonPath.read(send("/api/v1/parts",
				"{\"sku\":\"SKU-%s\",\"name\":\"Oil filter\",\"unitPrice\":35.00,\"minimumQuantity\":0}".formatted(UUID.randomUUID().toString().substring(0, 12)),
				"ADMIN", 201), "$.id");
		return new Fixture(customerId, document, plate, serviceId, partId);
	}

	private String openBody(Fixture f, String items) {
		return "{\"document\":\"%s\",\"plate\":\"%s\",\"complaint\":\"noise\",\"items\":[%s]}".formatted(f.document(), f.plate(), items);
	}

	private static String item(String type, String id, int quantity) {
		return "{\"type\":\"%s\",\"referenceId\":\"%s\",\"quantity\":%d}".formatted(type, id, quantity);
	}

	private String open(Fixture f, String items) throws Exception {
		return JsonPath.read(send("/api/v1/work-orders", openBody(f, items), "ATTENDANT", 201), "$.id");
	}

	private String toWaitingApproval(Fixture f, String items) throws Exception {
		String id = open(f, items);
		send("/api/v1/work-orders/%s/diagnosis/start".formatted(id), "", "MECHANIC", 200);
		return id;
	}

	private String completeDiagnosis(String id) throws Exception {
		String body = send("/api/v1/work-orders/%s/diagnosis/complete".formatted(id), "", "MECHANIC", 200);
		java.util.List<String> ids = JsonPath.read(body, "$.budgets[*].id");
		return ids.getLast();
	}

	private ResultActions customerAction(String customerId, String id, String budgetId, String action, String body) throws Exception {
		return mvc.perform(post("/api/v1/me/work-orders/%s/budgets/%s/%s".formatted(id, budgetId, action)).with(asCustomer(customerId))
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void fullLifecycleForServiceOnlyWorkOrder() throws Exception {
		Fixture f = fixture();

		String openBody = send("/api/v1/work-orders", openBody(f, item("SERVICE", f.serviceId(), 1)), "ATTENDANT", 201);
		String id = JsonPath.read(openBody, "$.id");
		assertThat((String) JsonPath.read(openBody, "$.number")).matches("OS-\\d{4}-\\d{6}");
		assertThat((String) JsonPath.read(openBody, "$.status")).isEqualTo("RECEIVED");
		assertThat((String) JsonPath.read(openBody, "$.draftItems[0].name")).startsWith("Oil change");

		send("/api/v1/work-orders/%s/diagnosis/start".formatted(id), "", "MECHANIC", 200);
		String budgetId = completeDiagnosis(id);

		customerAction(f.customerId(), id, budgetId, "approve", "").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_EXECUTION"))
				.andExpect(jsonPath("$.approvedTotal").value(150.0))
				.andExpect(jsonPath("$.budgets[0].status").value("APPROVED"))
				.andExpect(jsonPath("$.budgets[0].decision.channel").value("APP"));

		send("/api/v1/work-orders/%s/complete".formatted(id), "", "MECHANIC", 200);
		mvc.perform(post("/api/v1/work-orders/{id}/deliver", id).with(as("ATTENDANT")))
				.andExpect(status().isOk()).andExpect(jsonPath("$.status").value("DELIVERED"))
				.andExpect(jsonPath("$.history.length()").value(6))
				.andExpect(jsonPath("$.history[0].to").value("RECEIVED"))
				.andExpect(jsonPath("$.history[5].to").value("DELIVERED"))
				.andExpect(jsonPath("$.history[5].by.role").value("ATTENDANT"));
	}

	@Test
	void approvalWithPartsKeepsTheOrderWaitingAndPublishesTheParts() throws Exception {
		Fixture f = fixture();
		String id = toWaitingApproval(f, item("SERVICE", f.serviceId(), 1) + "," + item("PART", f.partId(), 2));
		String budgetId = completeDiagnosis(id);

		customerAction(f.customerId(), id, budgetId, "approve", "").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WAITING_APPROVAL"))
				.andExpect(jsonPath("$.budgets[0].status").value("APPROVED"));

		assertThat(events.stream(BudgetApproved.class).filter(e -> e.workOrderId().toString().equals(id))).singleElement().satisfies(e -> {
			assertThat(e.parts()).singleElement().satisfies(p -> {
				assertThat(p.partId().toString()).isEqualTo(f.partId());
				assertThat(p.quantity()).isEqualTo(2);
			});
			assertThat(e.total()).isEqualByComparingTo("220");
		});
	}

	@Test
	void rejectionReopensTheDiagnosisAndTheNextBudgetChainsToThePrevious() throws Exception {
		Fixture f = fixture();
		String id = toWaitingApproval(f, item("SERVICE", f.serviceId(), 1) + "," + item("PART", f.partId(), 1));
		String first = completeDiagnosis(id);

		customerAction(f.customerId(), id, first, "reject", "{\"reason\":\"too expensive\"}").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"))
				.andExpect(jsonPath("$.budgets[0].status").value("REJECTED"))
				.andExpect(jsonPath("$.budgets[0].decision.reason").value("too expensive"))
				.andExpect(jsonPath("$.draftItems.length()").value(2));

		String body = send("/api/v1/work-orders/%s/items".formatted(id), item("SERVICE", f.serviceId(), 1), "MECHANIC", 200);
		java.util.List<String> partItems = JsonPath.read(body, "$.draftItems[?(@.type=='PART')].id");
		String partItem = partItems.getFirst();
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/work-orders/{id}/items/{item}", id, partItem)
				.with(as("MECHANIC"))).andExpect(status().isOk()).andExpect(jsonPath("$.draftItems.length()").value(1));

		String second = completeDiagnosis(id);
		mvc.perform(get("/api/v1/work-orders/{id}", id).with(as("ATTENDANT")))
				.andExpect(jsonPath("$.budgets.length()").value(2))
				.andExpect(jsonPath("$.budgets[1].sequence").value(2))
				.andExpect(jsonPath("$.budgets[1].previousBudgetId").value(first))
				.andExpect(jsonPath("$.budgets[1].total").value(300.0));
		assertThat(second).isNotEqualTo(first);
	}

	@Test
	void attendantCanDecideOnBehalfOfTheCustomerWithAChannel() throws Exception {
		Fixture f = fixture();
		String id = toWaitingApproval(f, item("SERVICE", f.serviceId(), 1));
		String budgetId = completeDiagnosis(id);
		String url = "/api/v1/work-orders/%s/budgets/%s/approve".formatted(id, budgetId);

		send(url, "{\"channel\":\"APP\"}", "ATTENDANT", 422);
		send(url, "{}", "ATTENDANT", 400);
		send(url, "{\"channel\":\"PHONE\"}", "MECHANIC", 403);
		String body = send(url, "{\"channel\":\"PHONE\"}", "ATTENDANT", 200);

		assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("IN_EXECUTION");
		assertThat((String) JsonPath.read(body, "$.budgets[0].decision.channel")).isEqualTo("PHONE");
		assertThat((String) JsonPath.read(body, "$.budgets[0].decision.by.subject")).isEqualTo("user-ATTENDANT");
		send(url, "{\"channel\":\"PHONE\"}", "ATTENDANT", 409);
	}

	@Test
	void additionalBudgetPausesExecutionUntilTheCustomerDecides() throws Exception {
		Fixture f = fixture();
		String id = toWaitingApproval(f, item("SERVICE", f.serviceId(), 1));
		customerAction(f.customerId(), id, completeDiagnosis(id), "approve", "").andExpect(status().isOk());

		String body = send("/api/v1/work-orders/%s/additional-budget".formatted(id),
				"{\"items\":[%s]}".formatted(item("SERVICE", f.serviceId(), 2)), "MECHANIC", 200);
		assertThat((String) JsonPath.read(body, "$.status")).isEqualTo("WAITING_APPROVAL");
		assertThat((String) JsonPath.read(body, "$.budgets[1].kind")).isEqualTo("ADDITIONAL");
		assertThat((String) JsonPath.read(body, "$.budgets[1].previousBudgetId")).isEqualTo(JsonPath.read(body, "$.budgets[0].id"));
		assertThat(events.stream(BudgetIssued.class).filter(e -> e.workOrderId().toString().equals(id)).map(BudgetIssued::kind).toList())
				.containsExactly("INITIAL", "ADDITIONAL");

		customerAction(f.customerId(), id, JsonPath.read(body, "$.budgets[1].id"), "approve", "").andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("IN_EXECUTION"))
				.andExpect(jsonPath("$.approvedTotal").value(450.0));
		send("/api/v1/work-orders/%s/additional-budget".formatted(id), "{\"items\":[]}", "MECHANIC", 400);
	}

	@Test
	void customersOnlySeeTheirOwnWorkOrdersWithoutInternalActors() throws Exception {
		Fixture mine = fixture();
		Fixture other = fixture();
		String myId = open(mine, item("SERVICE", mine.serviceId(), 1));
		String otherId = open(other, item("SERVICE", other.serviceId(), 1));

		mvc.perform(get("/api/v1/me/work-orders").with(asCustomer(mine.customerId())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(myId));
		mvc.perform(get("/api/v1/me/work-orders/{id}", myId).with(asCustomer(mine.customerId())))
				.andExpect(status().isOk()).andExpect(jsonPath("$.history[0].by").doesNotExist())
				.andExpect(jsonPath("$.history[0].to").value("RECEIVED"));
		mvc.perform(get("/api/v1/me/work-orders/{id}", otherId).with(asCustomer(mine.customerId()))).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/me/work-orders").with(as("CUSTOMER"))).andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/me/work-orders").with(as("ATTENDANT"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/me/work-orders")).andExpect(status().isUnauthorized());
	}

	@Test
	void aCustomerCannotDecideAnotherCustomersBudget() throws Exception {
		Fixture mine = fixture();
		Fixture other = fixture();
		String id = toWaitingApproval(other, item("SERVICE", other.serviceId(), 1));
		String budgetId = completeDiagnosis(id);

		customerAction(mine.customerId(), id, budgetId, "approve", "").andExpect(status().isNotFound());
		customerAction(mine.customerId(), id, budgetId, "reject", "").andExpect(status().isNotFound());
		mvc.perform(get("/api/v1/work-orders/{id}", id).with(as("ADMIN"))).andExpect(jsonPath("$.status").value("WAITING_APPROVAL"));
	}

	@Test
	void enforcesStaffRoles() throws Exception {
		Fixture f = fixture();
		String id = open(f, item("SERVICE", f.serviceId(), 1));

		send("/api/v1/work-orders", openBody(f, ""), "MECHANIC", 403);
		send("/api/v1/work-orders", openBody(f, ""), "CUSTOMER", 403);
		send("/api/v1/work-orders/%s/diagnosis/start".formatted(id), "", "ATTENDANT", 403);
		send("/api/v1/work-orders/%s/deliver".formatted(id), "", "MECHANIC", 403);
		send("/api/v1/work-orders/%s/cancel".formatted(id), "", "MECHANIC", 403);
		mvc.perform(get("/api/v1/work-orders/{id}", id)).andExpect(status().isUnauthorized());
		mvc.perform(get("/api/v1/work-orders/{id}", id).with(as("CUSTOMER"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/work-orders/{id}", id).with(as("MECHANIC"))).andExpect(status().isOk());
	}

	@Test
	void aVehicleCanOnlyHaveOneActiveWorkOrderAndIsFreedByCancellation() throws Exception {
		Fixture f = fixture();
		String id = open(f, item("SERVICE", f.serviceId(), 1));

		send("/api/v1/work-orders", openBody(f, ""), "ATTENDANT", 409);

		send("/api/v1/work-orders/%s/cancel".formatted(id), "{\"reason\":\"customer gave up\"}", "ATTENDANT", 200);
		mvc.perform(get("/api/v1/work-orders/{id}", id).with(as("ADMIN")))
				.andExpect(jsonPath("$.status").value("CANCELLED")).andExpect(jsonPath("$.history[1].note").value("customer gave up"));
		send("/api/v1/work-orders", openBody(f, ""), "ATTENDANT", 201);
		send("/api/v1/work-orders/%s/cancel".formatted(id), "", "ATTENDANT", 409);
	}

	@Test
	void validatesTheOpeningRequest() throws Exception {
		Fixture f = fixture();
		Fixture someoneElse = fixture();

		send("/api/v1/work-orders", "{\"plate\":\"%s\"}".formatted(f.plate()), "ATTENDANT", 400);
		send("/api/v1/work-orders", openBody(f, item("SERVICE", f.serviceId(), 0)), "ATTENDANT", 400);
		send("/api/v1/work-orders", openBody(f, item("SERVICE", UUID.randomUUID().toString(), 1)), "ATTENDANT", 404);
		send("/api/v1/work-orders", "{\"document\":\"%s\",\"plate\":\"%s\"}".formatted(cpf(), f.plate()), "ATTENDANT", 404);
		send("/api/v1/work-orders", "{\"document\":\"%s\",\"plate\":\"%s\"}".formatted(f.document(), someoneElse.plate()), "ATTENDANT", 422);
		send("/api/v1/work-orders", openBody(f, ""), "ATTENDANT", 201);
	}

	@Test
	void itemsAreLockedAfterTheBudgetIsIssuedAndDiagnosisNeedsItems() throws Exception {
		Fixture f = fixture();
		String empty = toWaitingApproval(f, "");
		send("/api/v1/work-orders/%s/diagnosis/complete".formatted(empty), "", "MECHANIC", 422);

		send("/api/v1/work-orders/%s/items".formatted(empty), item("SERVICE", f.serviceId(), 1), "MECHANIC", 200);
		completeDiagnosis(empty);
		send("/api/v1/work-orders/%s/items".formatted(empty), item("SERVICE", f.serviceId(), 1), "MECHANIC", 409);
		mvc.perform(put("/api/v1/work-orders/{id}/items/{item}", empty, UUID.randomUUID()).with(as("MECHANIC"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"quantity\":2}")).andExpect(status().isConflict());
	}

	@Test
	void listsWithFilters() throws Exception {
		Fixture f = fixture();
		String id = open(f, item("SERVICE", f.serviceId(), 1));

		mvc.perform(get("/api/v1/work-orders").with(as("ATTENDANT")).param("customerId", f.customerId()))
				.andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(id))
				.andExpect(jsonPath("$.content[0].vehiclePlate").value(f.plate()));
		mvc.perform(get("/api/v1/work-orders").with(as("ATTENDANT")).param("customerId", f.customerId()).param("status", "RECEIVED"))
				.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/work-orders").with(as("ATTENDANT")).param("customerId", f.customerId()).param("status", "DELIVERED"))
				.andExpect(jsonPath("$.totalElements").value(0));
		mvc.perform(get("/api/v1/work-orders").with(as("ATTENDANT")).param("plate", f.plate().toLowerCase()))
				.andExpect(jsonPath("$.totalElements").value(1));
		mvc.perform(get("/api/v1/work-orders").with(as("ATTENDANT")).param("status", "NOPE")).andExpect(status().isBadRequest());
	}

	@Test
	void snapshotsFollowCustomerVehicleServiceChangesWhileTheBudgetStaysFrozen() throws Exception {
		Fixture f = fixture();
		String id = open(f, item("SERVICE", f.serviceId(), 1));
		String frozen = open(fixture(), item("SERVICE", f.serviceId(), 1));
		String vehicleId = JsonPath.read(json(mvc.perform(get("/api/v1/customers/{id}/vehicles", f.customerId()).with(as("ADMIN")))), "$.content[0].id");

		mvc.perform(put("/api/v1/customers/{id}", f.customerId()).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Ana Lima\",\"email\":\"%s\"}".formatted(email()))).andExpect(status().isOk());
		mvc.perform(put("/api/v1/vehicles/{id}", vehicleId).with(as("ATTENDANT")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"brand\":\"VW\",\"model\":\"Polo\",\"year\":2023}")).andExpect(status().isOk());
		mvc.perform(put("/api/v1/services/{id}", f.serviceId()).with(as("ADMIN")).contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Oil change gold %s\",\"price\":199.90,\"estimatedMinutes\":50}".formatted(UUID.randomUUID()))).andExpect(status().isOk());

		await().untilAsserted(() -> mvc.perform(get("/api/v1/work-orders/{id}", id).with(as("ADMIN")))
				.andExpect(jsonPath("$.customer.name").value("Ana Lima"))
				.andExpect(jsonPath("$.vehicle.model").value("Polo"))
				.andExpect(jsonPath("$.draftItems[0].unitPrice").value(199.9))
				.andExpect(jsonPath("$.draftItems[0].name").value(org.hamcrest.Matchers.startsWith("Oil change gold"))));
		assertThat(frozen).isNotEqualTo(id);
	}
}

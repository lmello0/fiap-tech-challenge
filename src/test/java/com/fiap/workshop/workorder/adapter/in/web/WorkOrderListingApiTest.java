package com.fiap.workshop.workorder.adapter.in.web;

import static com.fiap.workshop.support.TestDocuments.cpf;
import static com.fiap.workshop.support.TestDocuments.email;
import static com.fiap.workshop.support.TestDocuments.plate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.support.RecordingIdentityConfig;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class WorkOrderListingApiTest {

	@Autowired
	MockMvc mvc;

	private final String document = cpf();
	private String customerId;
	private String serviceId;
	private String firstReceived;
	private String inDiagnosis;
	private String waitingApproval;
	private String inExecution;
	private String completed;
	private String cancelled;
	private String secondReceived;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)).jwt(j -> j.subject("user-" + role));
	}

	private String send(String url, String body, String role) throws Exception {
		return mvc.perform(post(url).with(as(role)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
	}

	private String open() throws Exception {
		String body = send("/api/v1/work-orders", ("{\"customer\":{\"document\":\"%s\",\"name\":\"Ana\",\"email\":\"" + email() + "\"},"
				+ "\"vehicle\":{\"plate\":\"%s\",\"brand\":\"Fiat\",\"model\":\"Argo\",\"year\":2022},"
				+ "\"items\":[{\"type\":\"SERVICE\",\"referenceId\":\"%s\",\"quantity\":1}]}").formatted(document, plate(), serviceId), "ATTENDANT");
		customerId = JsonPath.read(body, "$.customer.id");
		return JsonPath.read(body, "$.id");
	}

	private String budgetOf(String body) {
		List<String> budgets = JsonPath.read(body, "$.budgets[*].id");
		return budgets.getLast();
	}

	private String waitingApproval() throws Exception {
		String id = open();
		send("/api/v1/work-orders/%s/diagnosis/start".formatted(id), "", "MECHANIC");
		return id;
	}

	private String executing() throws Exception {
		String id = waitingApproval();
		String budget = budgetOf(send("/api/v1/work-orders/%s/diagnosis/complete".formatted(id), "", "MECHANIC"));
		send("/api/v1/work-orders/%s/budgets/%s/approve".formatted(id, budget), "{\"channel\":\"PHONE\"}", "ATTENDANT");
		return id;
	}

	@BeforeEach
	void queue() throws Exception {
		serviceId = JsonPath.read(send("/api/v1/services", "{\"name\":\"Service %s\",\"price\":100,\"estimatedMinutes\":30}".formatted(UUID.randomUUID()), "ADMIN"), "$.id");
		firstReceived = open();
		inDiagnosis = waitingApproval();
		waitingApproval = waitingApproval();
		send("/api/v1/work-orders/%s/diagnosis/complete".formatted(waitingApproval), "", "MECHANIC");
		inExecution = executing();
		completed = executing();
		send("/api/v1/work-orders/%s/complete".formatted(completed), "", "MECHANIC");
		cancelled = open();
		send("/api/v1/work-orders/%s/cancel".formatted(cancelled), "{\"reason\":\"gave up\"}", "ATTENDANT");
		secondReceived = open();
	}

	private List<String> ids(String url, RequestPostProcessor auth, String... params) throws Exception {
		var request = get(url).with(auth).param("customerId", customerId);
		for (int i = 0; i < params.length; i += 2) {
			request = request.param(params[i], params[i + 1]);
		}
		String body = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return JsonPath.read(body, "$.content[*].id");
	}

	@Test
	void defaultListingOrdersByStatusPriorityThenOldestFirstAndHidesFinishedOrders() throws Exception {
		assertThat(ids("/api/v1/work-orders", as("ATTENDANT")))
				.containsExactly(inExecution, waitingApproval, inDiagnosis, firstReceived, secondReceived);
	}

	@Test
	void includeFinishedAddsTheClosedOrdersAfterTheOpenOnes() throws Exception {
		assertThat(ids("/api/v1/work-orders", as("ADMIN"), "includeFinished", "true"))
				.containsExactly(inExecution, waitingApproval, inDiagnosis, firstReceived, secondReceived, completed, cancelled);
	}

	@Test
	void anExplicitStatusAlwaysShowsThatStatus() throws Exception {
		assertThat(ids("/api/v1/work-orders", as("ADMIN"), "status", "COMPLETED")).containsExactly(completed);
		assertThat(ids("/api/v1/work-orders", as("ADMIN"), "status", "CANCELLED")).containsExactly(cancelled);
		assertThat(ids("/api/v1/work-orders", as("ADMIN"), "status", "RECEIVED")).containsExactly(firstReceived, secondReceived);
	}

	@Test
	void paginationFollowsTheSameOrder() throws Exception {
		assertThat(ids("/api/v1/work-orders", as("ADMIN"), "size", "2", "page", "1")).containsExactly(inDiagnosis, firstReceived);
		String body = mvc.perform(get("/api/v1/work-orders").with(as("ADMIN")).param("customerId", customerId).param("size", "2"))
				.andReturn().getResponse().getContentAsString();
		assertThat((Integer) JsonPath.read(body, "$.totalElements")).isEqualTo(5);
		assertThat((Integer) JsonPath.read(body, "$.totalPages")).isEqualTo(3);
	}

	@Test
	void theCustomerSeesTheirOwnOrdersInTheSameOrder() throws Exception {
		RequestPostProcessor customer = jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")).jwt(j -> j.claim("customer_id", customerId));

		assertThat(ids("/api/v1/me/work-orders", customer)).containsExactly(inExecution, waitingApproval, inDiagnosis, firstReceived, secondReceived);
		assertThat(ids("/api/v1/me/work-orders", customer, "includeFinished", "true")).hasSize(7).endsWith(completed, cancelled);
	}
}

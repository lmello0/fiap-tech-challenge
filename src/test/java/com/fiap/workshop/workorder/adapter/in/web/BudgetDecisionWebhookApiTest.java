package com.fiap.workshop.workorder.adapter.in.web;

import static com.fiap.workshop.support.TestDocuments.cpf;
import static com.fiap.workshop.support.TestDocuments.email;
import static com.fiap.workshop.support.TestDocuments.plate;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.support.RecordingIdentityConfig;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
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
class BudgetDecisionWebhookApiTest {

	private static final String SECRET = "demo-webhook-secret";
	private static final String URL = "/api/v1/webhooks/budget-decisions";

	@Autowired
	MockMvc mvc;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role)).jwt(j -> j.subject("user-" + role));
	}

	private String send(String url, String body, String role) throws Exception {
		return mvc.perform(post(url).with(as(role)).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().is2xxSuccessful()).andReturn().getResponse().getContentAsString();
	}

	private record Pending(String workOrderId, String number, String budgetId) {
	}

	private Pending pendingBudget() throws Exception {
		String service = JsonPath.read(send("/api/v1/services", "{\"name\":\"Service %s\",\"price\":100,\"estimatedMinutes\":30}".formatted(UUID.randomUUID()), "ADMIN"), "$.id");
		String opened = send("/api/v1/work-orders", ("{\"customer\":{\"document\":\"%s\",\"name\":\"Ana\",\"email\":\"" + email() + "\"},"
				+ "\"vehicle\":{\"plate\":\"%s\",\"brand\":\"Fiat\",\"model\":\"Argo\",\"year\":2022},"
				+ "\"items\":[{\"type\":\"SERVICE\",\"referenceId\":\"%s\",\"quantity\":1}]}").formatted(cpf(), plate(), service), "ATTENDANT");
		String id = JsonPath.read(opened, "$.id");
		send("/api/v1/work-orders/%s/diagnosis/start".formatted(id), "", "MECHANIC");
		String body = send("/api/v1/work-orders/%s/diagnosis/complete".formatted(id), "", "MECHANIC");
		List<String> budgets = JsonPath.read(body, "$.budgets[*].id");
		return new Pending(id, JsonPath.read(body, "$.number"), budgets.getLast());
	}

	private static String payload(UUID eventId, Pending p, String decision, String reason) {
		return "{\"eventId\":\"%s\",\"workOrderNumber\":\"%s\",\"budgetId\":\"%s\",\"decision\":\"%s\"%s}".formatted(eventId, p.number(), p.budgetId(),
				decision, reason == null ? "" : ",\"reason\":\"" + reason + "\"");
	}

	private ResultActions webhook(String body, String timestamp, String signature) throws Exception {
		var request = post(URL).contentType(MediaType.APPLICATION_JSON).content(body);
		if (timestamp != null) {
			request = request.header("X-Timestamp", timestamp);
		}
		if (signature != null) {
			request = request.header("X-Signature", signature);
		}
		return mvc.perform(request);
	}

	private ResultActions signed(String body) throws Exception {
		String ts = String.valueOf(Instant.now().getEpochSecond());
		return webhook(body, ts, "sha256=" + WebhookSignatureVerifier.sign(SECRET, ts + "." + body));
	}

	private ResultActions order(Pending p) throws Exception {
		return mvc.perform(get("/api/v1/work-orders/{id}", p.workOrderId()).with(as("ADMIN")));
	}

	@Test
	void aSignedApprovalMovesTheOrderToExecutionWithoutAnyJwt() throws Exception {
		Pending p = pendingBudget();

		signed(payload(UUID.randomUUID(), p, "APPROVED", null)).andExpect(status().isOk()).andExpect(jsonPath("$.result").value("APPLIED"));

		order(p).andExpect(jsonPath("$.status").value("IN_EXECUTION"))
				.andExpect(jsonPath("$.budgets[0].status").value("APPROVED"))
				.andExpect(jsonPath("$.budgets[0].decision.channel").value("EXTERNAL"))
				.andExpect(jsonPath("$.budgets[0].decision.by.role").value("EXTERNAL"))
				.andExpect(jsonPath("$.history[-1:].by.role").value("EXTERNAL"));
	}

	@Test
	void aSignedRejectionReturnsTheOrderToDiagnosis() throws Exception {
		Pending p = pendingBudget();

		signed(payload(UUID.randomUUID(), p, "REJECTED", "too expensive")).andExpect(status().isOk());

		order(p).andExpect(jsonPath("$.status").value("IN_DIAGNOSIS"))
				.andExpect(jsonPath("$.budgets[0].decision.reason").value("too expensive"))
				.andExpect(jsonPath("$.budgets[0].decision.channel").value("EXTERNAL"));
	}

	@Test
	void repeatingTheSameEventIsHarmless() throws Exception {
		Pending p = pendingBudget();
		String body = payload(UUID.randomUUID(), p, "APPROVED", null);

		signed(body).andExpect(jsonPath("$.result").value("APPLIED"));
		signed(body).andExpect(status().isOk()).andExpect(jsonPath("$.result").value("DUPLICATE"));

		order(p).andExpect(jsonPath("$.status").value("IN_EXECUTION")).andExpect(jsonPath("$.history.length()").value(4));
	}

	@Test
	void aNewEventForAnAlreadyDecidedBudgetIsAConflict() throws Exception {
		Pending p = pendingBudget();
		signed(payload(UUID.randomUUID(), p, "APPROVED", null)).andExpect(status().isOk());

		signed(payload(UUID.randomUUID(), p, "REJECTED", null)).andExpect(status().isConflict());

		order(p).andExpect(jsonPath("$.status").value("IN_EXECUTION"));
	}

	@Test
	void unsignedBadlySignedOrExpiredRequestsAreRefusedAndChangeNothing() throws Exception {
		Pending p = pendingBudget();
		String body = payload(UUID.randomUUID(), p, "APPROVED", null);
		String ts = String.valueOf(Instant.now().getEpochSecond());
		String oldTs = String.valueOf(Instant.now().minusSeconds(600).getEpochSecond());

		webhook(body, null, null).andExpect(status().isUnauthorized());
		webhook(body, ts, null).andExpect(status().isUnauthorized());
		webhook(body, ts, "sha256=" + WebhookSignatureVerifier.sign("wrong-secret", ts + "." + body)).andExpect(status().isUnauthorized());
		webhook(body + " ", ts, "sha256=" + WebhookSignatureVerifier.sign(SECRET, ts + "." + body)).andExpect(status().isUnauthorized());
		webhook(body, oldTs, "sha256=" + WebhookSignatureVerifier.sign(SECRET, oldTs + "." + body)).andExpect(status().isUnauthorized());

		order(p).andExpect(jsonPath("$.status").value("WAITING_APPROVAL"));
	}

	@Test
	void signedButInvalidPayloadsAreBadRequestsAndUnknownTargetsAreNotFound() throws Exception {
		Pending p = pendingBudget();

		signed("not json").andExpect(status().isBadRequest());
		signed("{\"workOrderNumber\":\"" + p.number() + "\"}").andExpect(status().isBadRequest());
		signed(payload(UUID.randomUUID(), p, "MAYBE", null)).andExpect(status().isBadRequest());
		signed("{\"eventId\":\"" + UUID.randomUUID() + "\",\"workOrderNumber\":\"OS-0000-000000\",\"budgetId\":\"" + p.budgetId()
				+ "\",\"decision\":\"APPROVED\"}").andExpect(status().isNotFound());
		signed("{\"eventId\":\"" + UUID.randomUUID() + "\",\"workOrderNumber\":\"" + p.number() + "\",\"budgetId\":\"" + UUID.randomUUID()
				+ "\",\"decision\":\"APPROVED\"}").andExpect(status().isNotFound());

		order(p).andExpect(jsonPath("$.status").value("WAITING_APPROVAL"));
	}

	@Test
	void staffCannotRecordTheExternalChannelThroughTheStaffEndpoint() throws Exception {
		Pending p = pendingBudget();

		mvc.perform(post("/api/v1/work-orders/{id}/budgets/{budget}/approve", p.workOrderId(), p.budgetId()).with(as("ATTENDANT"))
				.contentType(MediaType.APPLICATION_JSON).content("{\"channel\":\"EXTERNAL\"}")).andExpect(status().isUnprocessableContent());
	}
}

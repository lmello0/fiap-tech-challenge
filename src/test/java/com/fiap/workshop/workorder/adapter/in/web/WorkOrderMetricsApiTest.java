package com.fiap.workshop.workorder.adapter.in.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.support.RecordingIdentityConfig;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class WorkOrderMetricsApiTest {

	private static final String FROM = "2020-01-01T00:00:00Z";
	private static final String TO = "2020-02-01T00:00:00Z";

	@Autowired
	MockMvc mvc;
	@Autowired
	JdbcClient jdbc;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@BeforeEach
	void history() {
		jdbc.sql("delete from workorder.work_order_status_history where work_order_id in (select id from workorder.work_order where number like 'MET-%')").update();
		jdbc.sql("delete from workorder.work_order where number like 'MET-%'").update();

		insertOrder("MET-A", "COMPLETED", new String[][] {
				{null, "RECEIVED", "2020-01-10T09:00:00Z"}, {"RECEIVED", "IN_DIAGNOSIS", "2020-01-10T09:10:00Z"},
				{"IN_DIAGNOSIS", "WAITING_APPROVAL", "2020-01-10T09:40:00Z"}, {"WAITING_APPROVAL", "IN_EXECUTION", "2020-01-10T10:00:00Z"},
				{"IN_EXECUTION", "WAITING_APPROVAL", "2020-01-10T10:30:00Z"}, {"WAITING_APPROVAL", "IN_EXECUTION", "2020-01-10T10:40:00Z"},
				{"IN_EXECUTION", "COMPLETED", "2020-01-10T11:00:00Z"}});
		insertOrder("MET-B", "COMPLETED", new String[][] {
				{null, "RECEIVED", "2020-01-12T09:00:00Z"}, {"RECEIVED", "IN_DIAGNOSIS", "2020-01-12T09:20:00Z"},
				{"IN_DIAGNOSIS", "WAITING_APPROVAL", "2020-01-12T09:50:00Z"}, {"WAITING_APPROVAL", "IN_EXECUTION", "2020-01-12T10:00:00Z"},
				{"IN_EXECUTION", "COMPLETED", "2020-01-12T10:30:00Z"}});
		insertOrder("MET-ONGOING", "IN_EXECUTION", new String[][] {
				{null, "RECEIVED", "2020-01-14T09:00:00Z"}, {"RECEIVED", "IN_EXECUTION", "2020-01-14T09:05:00Z"}});
		insertOrder("MET-OUTSIDE", "COMPLETED", new String[][] {
				{null, "RECEIVED", "2019-06-01T09:00:00Z"}, {"RECEIVED", "IN_EXECUTION", "2019-06-01T09:10:00Z"},
				{"IN_EXECUTION", "COMPLETED", "2019-06-01T19:10:00Z"}});
	}

	private void insertOrder(String number, String status, String[][] changes) {
		UUID id = UUID.randomUUID();
		jdbc.sql("""
				insert into workorder.work_order (id, number, status, customer_id, customer_name, customer_document, customer_email,
				  vehicle_id, vehicle_plate, vehicle_brand, vehicle_model, vehicle_year, created_at, updated_at)
				values (?, ?, ?, ?, 'Metric Customer', '52998224725', 'm@example.com', ?, 'MET0000', 'Fiat', 'Argo', 2022, now(), now())
				""").params(id, number, status, UUID.randomUUID(), UUID.randomUUID()).update();
		for (String[] change : changes) {
			jdbc.sql("insert into workorder.work_order_status_history (id, work_order_id, from_status, to_status, occurred_at, actor_subject, actor_role) "
					+ "values (?, ?, ?, ?, ?, 'test', 'SYSTEM')")
					.params(UUID.randomUUID(), id, change[0], change[1], Timestamp.from(Instant.parse(change[2]))).update();
		}
	}

	@Test
	void averagesTheNetExecutionTimeOfOrdersCompletedInThePeriod() throws Exception {
		mvc.perform(get("/api/v1/work-orders/metrics").with(as("ATTENDANT")).param("from", FROM).param("to", TO))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.completedWorkOrders").value(2))
				.andExpect(jsonPath("$.averageExecutionSeconds").value(2400))
				.andExpect(jsonPath("$.averageExecution").value("PT40M"));
	}

	@Test
	void reportsTheAverageTimeSpentInEachStatus() throws Exception {
		mvc.perform(get("/api/v1/work-orders/metrics").with(as("ADMIN")).param("from", FROM).param("to", TO))
				.andExpect(jsonPath("$.averageTimePerStatus[?(@.status=='RECEIVED')].averageSeconds").value(900))
				.andExpect(jsonPath("$.averageTimePerStatus[?(@.status=='IN_DIAGNOSIS')].averageSeconds").value(1800))
				.andExpect(jsonPath("$.averageTimePerStatus[?(@.status=='WAITING_APPROVAL')].averageSeconds").value(1200))
				.andExpect(jsonPath("$.averageTimePerStatus[?(@.status=='IN_EXECUTION')].workOrders").value(2))
				.andExpect(jsonPath("$.averageTimePerStatus[?(@.status=='COMPLETED')]").isEmpty());
	}

	@Test
	void anEmptyPeriodHasZeroValues() throws Exception {
		mvc.perform(get("/api/v1/work-orders/metrics").with(as("ADMIN")).param("from", "2000-01-01T00:00:00Z").param("to", "2000-02-01T00:00:00Z"))
				.andExpect(jsonPath("$.completedWorkOrders").value(0))
				.andExpect(jsonPath("$.averageExecutionSeconds").value(0))
				.andExpect(jsonPath("$.averageTimePerStatus").isEmpty());
	}

	@Test
	void validatesThePeriodAndTheRoles() throws Exception {
		mvc.perform(get("/api/v1/work-orders/metrics").with(as("ADMIN")).param("from", TO).param("to", FROM)).andExpect(status().isUnprocessableContent());
		mvc.perform(get("/api/v1/work-orders/metrics").with(as("ADMIN")).param("from", "yesterday")).andExpect(status().isBadRequest());
		mvc.perform(get("/api/v1/work-orders/metrics").with(as("MECHANIC"))).andExpect(status().isOk());
		mvc.perform(get("/api/v1/work-orders/metrics").with(as("CUSTOMER"))).andExpect(status().isForbidden());
		mvc.perform(get("/api/v1/work-orders/metrics")).andExpect(status().isUnauthorized());
	}
}

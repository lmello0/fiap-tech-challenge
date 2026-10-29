package com.fiap.workshop.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fiap.workshop.TestcontainersConfiguration;
import com.fiap.workshop.support.RecordingIdentityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@Import({TestcontainersConfiguration.class, RecordingIdentityConfig.class})
class DemoSeedTest {

	private static final String DEMO_CUSTOMER = "0198a000-0000-7000-8000-000000000001";

	@Autowired
	MockMvc mvc;
	@Autowired
	JdbcClient jdbc;

	private static RequestPostProcessor as(String role) {
		return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@Test
	void seedsEveryModule() {
		assertThat(jdbc.sql("select count(*) from customer.customer").query(Long.class).single()).isEqualTo(2);
		assertThat(jdbc.sql("select count(*) from customer.vehicle").query(Long.class).single()).isEqualTo(3);
		assertThat(jdbc.sql("select count(*) from catalog.repair_service").query(Long.class).single()).isEqualTo(4);
		assertThat(jdbc.sql("select count(*) from inventory.part").query(Long.class).single()).isEqualTo(3);
		assertThat(jdbc.sql("select count(*) from workorder.work_order").query(Long.class).single()).isEqualTo(3);
	}

	@Test
	void theDemoCustomerMatchesTheKeycloakUserClaim() throws Exception {
		mvc.perform(get("/api/v1/me").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")).jwt(j -> j.claim("customer_id", DEMO_CUSTOMER))))
				.andExpect(status().isOk()).andExpect(jsonPath("$.email").value("customer@workshop.local"));
		mvc.perform(get("/api/v1/me/work-orders").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER")).jwt(j -> j.claim("customer_id", DEMO_CUSTOMER))))
				.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	void seededWorkOrdersAreReadableWithTheirBudgetsAndHistory() throws Exception {
		mvc.perform(get("/api/v1/work-orders/{id}", "0198a000-0000-7000-8000-000000000502").with(as("ATTENDANT")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("WAITING_APPROVAL"))
				.andExpect(jsonPath("$.budgets[0].status").value("PENDING"))
				.andExpect(jsonPath("$.budgets[0].total").value(300.0))
				.andExpect(jsonPath("$.budgets[0].lines.length()").value(2))
				.andExpect(jsonPath("$.history.length()").value(3));
		mvc.perform(get("/api/v1/work-orders/{id}", "0198a000-0000-7000-8000-000000000503").with(as("ATTENDANT")))
				.andExpect(jsonPath("$.status").value("DELIVERED")).andExpect(jsonPath("$.approvedTotal").value(185.0));
	}

	@Test
	void theDeliveredOrderFeedsTheMetrics() throws Exception {
		mvc.perform(get("/api/v1/work-orders/metrics").with(as("ADMIN")))
				.andExpect(jsonPath("$.completedWorkOrders").value(1))
				.andExpect(jsonPath("$.averageExecution").value("PT45M"));
	}

	@Test
	void lowStockPartsShowUpInTheStockList() throws Exception {
		mvc.perform(get("/api/v1/parts").with(as("ADMIN")).param("lowStockOnly", "true"))
				.andExpect(jsonPath("$.totalElements").value(2));
	}
}

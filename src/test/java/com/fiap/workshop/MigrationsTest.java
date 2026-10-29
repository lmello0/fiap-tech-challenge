package com.fiap.workshop;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class MigrationsTest {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void everyModuleOwnsASchemaWithItsOwnFlywayHistory() {
		List<String> historyTables = jdbc.queryForList(
				"SELECT table_schema FROM information_schema.tables WHERE table_name = 'flyway_schema_history'", String.class);

		assertThat(historyTables).containsExactlyInAnyOrder("events", "customer", "catalog", "inventory", "workorder", "notification");
	}

	@Test
	void eventPublicationRegistryLivesInItsOwnSchema() {
		Integer count = jdbc.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_schema = 'events' AND table_name = 'event_publication'",
				Integer.class);

		assertThat(count).isEqualTo(1);
	}
}

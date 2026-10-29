package com.fiap.workshop.shared.config;

import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

class ModuleMigrations implements InitializingBean {

	static final List<String> SCHEMAS = List.of("events", "customer", "catalog", "inventory", "workorder", "notification");

	private final DataSource dataSource;
	private final boolean demo;

	ModuleMigrations(DataSource dataSource, Environment environment) {
		this.dataSource = dataSource;
		this.demo = environment.acceptsProfiles(Profiles.of("demo"));
	}

	@Override
	public void afterPropertiesSet() {
		SCHEMAS.forEach(this::migrate);
	}

	private void migrate(String schema) {
		List<String> locations = demo
				? List.of("classpath:db/migration/" + schema, "classpath:db/demo/" + schema)
				: List.of("classpath:db/migration/" + schema);
		Flyway.configure()
				.dataSource(dataSource)
				.schemas(schema)
				.defaultSchema(schema)
				.createSchemas(true)
				.locations(locations.toArray(String[]::new))
				.load()
				.migrate();
	}
}

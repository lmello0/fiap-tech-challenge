package com.fiap.workshop.shared.config;

import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * Runs one Flyway instance per module. Each module owns a schema, its migrations under
 * {@code db/migration/<module>} and its own {@code flyway_schema_history} table inside that schema, so a module
 * can be extracted with its data. Demo seed data lives under {@code db/demo/<module>} and is only applied with
 * the {@code demo} profile.
 */
class ModuleMigrations implements InitializingBean {

	/** Schemas that carry migrations, in dependency-free order (modules never reference each other's tables). */
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

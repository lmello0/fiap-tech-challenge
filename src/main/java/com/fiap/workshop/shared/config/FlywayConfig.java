package com.fiap.workshop.shared.config;

import javax.sql.DataSource;
import org.springframework.boot.jpa.autoconfigure.EntityManagerFactoryDependsOnPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
class FlywayConfig {

	@Bean
	ModuleMigrations moduleMigrations(DataSource dataSource, Environment environment) {
		return new ModuleMigrations(dataSource, environment);
	}

	/** Entity manager (and Hibernate schema validation) must start only after every module is migrated. */
	@Bean
	static EntityManagerFactoryDependsOnPostProcessor moduleMigrationsDependency() {
		return new EntityManagerFactoryDependsOnPostProcessor("moduleMigrations");
	}
}

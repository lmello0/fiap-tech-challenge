package com.fiap.techchallenge;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    public PostgreSQLContainer postgresContainer() {
        // Pinned to match compose.yaml: the migrations call uuidv7(), which needs Postgres 18.
        return new PostgreSQLContainer(DockerImageName.parse("postgres:18"));
    }

}

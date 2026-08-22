package com.fiap.techchallenge.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth.email")
public record AuthEmailProperties(String baseUrl) {
}

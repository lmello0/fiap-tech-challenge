package com.fiap.techchallenge.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.auth.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {
}

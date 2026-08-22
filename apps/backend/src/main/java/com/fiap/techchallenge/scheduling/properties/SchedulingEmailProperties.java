package com.fiap.techchallenge.scheduling.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mirrors {@code auth.properties.AuthEmailProperties} — the SPA origin that guest links point at. */
@ConfigurationProperties(prefix = "app.scheduling.email")
public record SchedulingEmailProperties(
        String baseUrl
) {
}

package com.fiap.techchallenge.workorder.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mirrors {@code scheduling.properties.SchedulingEmailProperties} — the SPA origin the Budget
 * decision link points at. */
@ConfigurationProperties(prefix = "app.workorder.email")
public record WorkOrderEmailProperties(
        String baseUrl
) {
}

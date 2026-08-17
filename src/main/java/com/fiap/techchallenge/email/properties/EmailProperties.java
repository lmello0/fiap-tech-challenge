package com.fiap.techchallenge.email.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.mail")
public record EmailProperties(
        String from,
        String fromName,
        int maxAttempts,
        Duration retryDelay
) {
}

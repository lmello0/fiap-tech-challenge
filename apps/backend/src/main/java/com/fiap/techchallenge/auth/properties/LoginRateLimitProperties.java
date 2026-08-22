package com.fiap.techchallenge.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.rate-limit")
public record LoginRateLimitProperties(
        int maxAttempts,
        Duration lockDuration
) {
}

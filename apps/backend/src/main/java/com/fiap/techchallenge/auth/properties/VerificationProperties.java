package com.fiap.techchallenge.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.verification")
public record VerificationProperties(
        Duration passwordResetTTL,
        Duration emailVerificationTTL,
        Duration emailChangeTTL
) {
}

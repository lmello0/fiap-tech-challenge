package com.fiap.techchallenge.shared.events;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.events")
public record EventPublicationProperties(Duration retention) {
}

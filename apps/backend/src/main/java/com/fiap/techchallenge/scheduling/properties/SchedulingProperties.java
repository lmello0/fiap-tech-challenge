package com.fiap.techchallenge.scheduling.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.scheduling")
public record SchedulingProperties(
        Duration minNotice,
        Duration maxLookahead,
        Duration accessTokenTtl,
        Duration registrationTokenTtl,
        Duration pickupInvitationTokenTtl
) {
}

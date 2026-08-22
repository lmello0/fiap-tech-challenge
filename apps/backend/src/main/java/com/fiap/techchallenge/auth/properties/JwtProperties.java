package com.fiap.techchallenge.auth.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.auth.jwt")
public record JwtProperties(
        String secret,
        String issuer,
        Duration accessTokenTTL,
        Duration refreshTokenTTL
) {
    public JwtProperties {
        if (secret == null || secret.getBytes().length < 32) {
            throw new IllegalStateException("app.auth.jwt.secret must be set and at least 32 bytes long for HS256 signing");
        }
    }
}

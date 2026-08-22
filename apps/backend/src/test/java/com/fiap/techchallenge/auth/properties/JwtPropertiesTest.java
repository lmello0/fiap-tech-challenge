package com.fiap.techchallenge.auth.properties;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void rejectsAMissingSecret() {
        assertThatThrownBy(() -> new JwtProperties(null, "issuer", Duration.ofMinutes(15), Duration.ofDays(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.auth.jwt.secret");
    }

    @Test
    void rejectsASecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtProperties("short-secret", "issuer", Duration.ofMinutes(15), Duration.ofDays(30)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.auth.jwt.secret");
    }
}

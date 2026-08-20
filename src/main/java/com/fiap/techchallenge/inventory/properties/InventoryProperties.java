package com.fiap.techchallenge.inventory.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.inventory")
public record InventoryProperties(
        Duration reservationTtl,
        int averageWindow,
        Vendor vendor
) {
    public record Vendor(Mock mock) {
        public record Mock(int leadTimeDays) {
        }
    }
}

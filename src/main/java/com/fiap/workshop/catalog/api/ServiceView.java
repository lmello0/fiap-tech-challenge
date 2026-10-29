package com.fiap.workshop.catalog.api;

import java.math.BigDecimal;
import java.util.UUID;

public record ServiceView(UUID id, String name, BigDecimal price, int estimatedMinutes, boolean active) {
}

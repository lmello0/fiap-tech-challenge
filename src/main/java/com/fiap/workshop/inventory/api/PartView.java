package com.fiap.workshop.inventory.api;

import java.math.BigDecimal;
import java.util.UUID;

public record PartView(UUID id, String sku, String name, BigDecimal unitPrice, boolean active) {
}

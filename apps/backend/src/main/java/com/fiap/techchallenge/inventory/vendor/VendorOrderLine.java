package com.fiap.techchallenge.inventory.vendor;

import java.math.BigDecimal;
import java.util.UUID;

public record VendorOrderLine(UUID partId, BigDecimal quantity) {
}

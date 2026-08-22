package com.fiap.techchallenge.inventory.vendor;

import java.util.List;
import java.util.UUID;

public record VendorOrderRequest(UUID vendorId, List<VendorOrderLine> lines) {
}

package com.fiap.techchallenge.inventory.vendor;

import java.time.Instant;

public record VendorOrderConfirmation(String vendorOrderRef, Instant expectedAt) {
}

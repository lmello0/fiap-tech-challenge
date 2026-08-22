package com.fiap.techchallenge.inventory.vendor;

import com.fiap.techchallenge.inventory.properties.InventoryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Stands in for a vendor's ordering system: confirms every order immediately with a synthetic
 * reference and an ETA {@code app.inventory.vendor.mock.lead-time-days} out. Package-private —
 * nothing outside this package may reach it directly; callers depend on {@link PartVendorClient}.
 */
@Component
@RequiredArgsConstructor
class MockPartVendorClient implements PartVendorClient {

    private final InventoryProperties properties;

    @Override
    public VendorOrderConfirmation placeOrder(VendorOrderRequest request) {
        String reference = "VEND-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Instant expectedAt = Instant.now().plus(Duration.ofDays(properties.vendor().mock().leadTimeDays()));

        return new VendorOrderConfirmation(reference, expectedAt);
    }
}

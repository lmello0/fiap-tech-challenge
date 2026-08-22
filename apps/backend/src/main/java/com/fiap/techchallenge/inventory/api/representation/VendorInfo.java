package com.fiap.techchallenge.inventory.api.representation;

import java.time.Instant;
import java.util.UUID;

public record VendorInfo(
        UUID id,
        String name,
        String contactEmail,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}

package com.fiap.workshop.workorder.api.events;

import java.util.UUID;

public record PartQuantity(UUID partId, int quantity) {
}

package com.fiap.workshop.inventory.application.port.in;

import java.util.UUID;

public record PartQuantity(UUID partId, int quantity) {
}

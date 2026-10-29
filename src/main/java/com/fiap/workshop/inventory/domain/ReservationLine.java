package com.fiap.workshop.inventory.domain;

import java.util.UUID;

public record ReservationLine(UUID partId, int quantity) {
}

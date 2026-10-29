package com.fiap.workshop.workorder.domain;

import java.util.UUID;

public record PartQuantity(UUID partId, int quantity) {
}

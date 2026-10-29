package com.fiap.workshop.inventory.api.events;

import java.util.UUID;

public record ShortagePart(UUID partId, int requested, int available) {
}

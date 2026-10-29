package com.fiap.workshop.workorder.domain;

import java.util.UUID;

public record CustomerSnapshot(UUID id, String name, String document, String email) {
}

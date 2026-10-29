package com.fiap.workshop.customer.api;

import java.util.UUID;

public record CustomerView(UUID id, String name, String document, String email, String phone, boolean active) {
}

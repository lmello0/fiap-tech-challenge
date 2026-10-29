package com.fiap.workshop.shared.domain;

public record Actor(String subject, String role) {

	public static final Actor SYSTEM = new Actor("system", "SYSTEM");
}

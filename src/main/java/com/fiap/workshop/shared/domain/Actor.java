package com.fiap.workshop.shared.domain;

public record Actor(String subject, String role) {

	public static final Actor SYSTEM = new Actor("system", "SYSTEM");
	public static final Actor EXTERNAL = new Actor("external", "EXTERNAL");
}

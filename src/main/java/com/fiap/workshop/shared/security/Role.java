package com.fiap.workshop.shared.security;

public enum Role {
	ADMIN, ATTENDANT, MECHANIC, CUSTOMER;

	public String authority() {
		return "ROLE_" + name();
	}
}

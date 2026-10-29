package com.fiap.workshop.shared.security;

/** Realm roles issued by Keycloak. */
public enum Role {
	ADMIN, ATTENDANT, MECHANIC, CUSTOMER;

	public String authority() {
		return "ROLE_" + name();
	}
}

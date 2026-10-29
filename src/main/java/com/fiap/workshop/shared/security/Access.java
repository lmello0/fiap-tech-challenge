package com.fiap.workshop.shared.security;

public final class Access {

	public static final String STAFF_WRITE = "hasAnyRole('ADMIN','ATTENDANT')";
	public static final String STAFF_READ = "hasAnyRole('ADMIN','ATTENDANT','MECHANIC')";
	public static final String CUSTOMER = "hasRole('CUSTOMER')";

	private Access() {
	}
}

package com.fiap.workshop.shared.domain;

import java.util.regex.Pattern;

public record Email(String value) {

	private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

	public Email {
		String normalized = value == null ? "" : value.trim().toLowerCase();
		if (normalized.length() > 254 || !FORMAT.matcher(normalized).matches()) {
			throw new DomainException("Invalid e-mail: " + value);
		}
		value = normalized;
	}

	public static Email of(String raw) {
		return new Email(raw);
	}
}

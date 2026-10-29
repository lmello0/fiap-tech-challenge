package com.fiap.workshop.shared.domain;

import java.util.regex.Pattern;

/** Brazilian license plate, either the legacy (ABC1234) or the Mercosul (ABC1D23) format, stored upper-case. */
public record LicensePlate(String value) {

	private static final Pattern LEGACY = Pattern.compile("[A-Z]{3}[0-9]{4}");
	private static final Pattern MERCOSUL = Pattern.compile("[A-Z]{3}[0-9][A-Z][0-9]{2}");

	public LicensePlate {
		String normalized = value == null ? "" : value.replaceAll("[\\s-]", "").toUpperCase();
		if (!LEGACY.matcher(normalized).matches() && !MERCOSUL.matcher(normalized).matches()) {
			throw new DomainException("Invalid license plate: " + value);
		}
		value = normalized;
	}

	public static LicensePlate of(String raw) {
		return new LicensePlate(raw);
	}
}

package com.fiap.workshop.customer.domain;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.LicensePlate;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Clock;
import java.time.Instant;
import java.time.Year;
import java.util.UUID;

public class Vehicle {

	private static final int OLDEST_YEAR = 1900;

	private final UUID id;
	private final UUID customerId;
	private final LicensePlate plate;
	private final Instant createdAt;
	private String brand;
	private String model;
	private int year;
	private boolean active;
	private Instant updatedAt;

	private Vehicle(UUID id, UUID customerId, LicensePlate plate, String brand, String model, int year, boolean active,
			Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.customerId = customerId;
		this.plate = plate;
		this.brand = requireText(brand, "brand");
		this.model = requireText(model, "model");
		this.year = year;
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static Vehicle register(UUID customerId, LicensePlate plate, String brand, String model, int year, Clock clock) {
		requireValidYear(year, clock);
		Instant now = clock.instant();
		return new Vehicle(UuidV7.generate(), customerId, plate, brand, model, year, true, now, now);
	}

	public static Vehicle restore(UUID id, UUID customerId, LicensePlate plate, String brand, String model, int year,
			boolean active, Instant createdAt, Instant updatedAt) {
		return new Vehicle(id, customerId, plate, brand, model, year, active, createdAt, updatedAt);
	}

	public void update(String brand, String model, int year, Clock clock) {
		requireActive();
		requireValidYear(year, clock);
		this.brand = requireText(brand, "brand");
		this.model = requireText(model, "model");
		this.year = year;
		this.updatedAt = clock.instant();
	}

	public void deactivate(Instant now) {
		requireActive();
		this.active = false;
		this.updatedAt = now;
	}

	public void requireActive() {
		if (!active) {
			throw new DomainException("Vehicle " + id + " is deactivated");
		}
	}

	private static void requireValidYear(int year, Clock clock) {
		int latest = Year.now(clock).getValue() + 1;
		if (year < OLDEST_YEAR || year > latest) {
			throw new DomainException("Vehicle year must be between " + OLDEST_YEAR + " and " + latest);
		}
	}

	private static String requireText(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new DomainException("Vehicle " + field + " is required");
		}
		return value.trim();
	}

	public UUID id() {
		return id;
	}

	public UUID customerId() {
		return customerId;
	}

	public LicensePlate plate() {
		return plate;
	}

	public String brand() {
		return brand;
	}

	public String model() {
		return model;
	}

	public int year() {
		return year;
	}

	public boolean active() {
		return active;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}
}

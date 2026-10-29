package com.fiap.workshop.catalog.domain;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;
import java.util.UUID;

public class RepairService {

	private final UUID id;
	private final Instant createdAt;
	private String name;
	private String description;
	private Money price;
	private int estimatedMinutes;
	private boolean active;
	private Instant updatedAt;

	private RepairService(UUID id, String name, String description, Money price, int estimatedMinutes, boolean active,
			Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.name = requireName(name);
		this.description = normalize(description);
		this.price = requireValidPrice(price);
		this.estimatedMinutes = requirePositive(estimatedMinutes);
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static RepairService create(String name, String description, Money price, int estimatedMinutes, Instant now) {
		return new RepairService(UuidV7.generate(), name, description, price, estimatedMinutes, true, now, now);
	}

	public static RepairService restore(UUID id, String name, String description, Money price, int estimatedMinutes,
			boolean active, Instant createdAt, Instant updatedAt) {
		return new RepairService(id, name, description, price, estimatedMinutes, active, createdAt, updatedAt);
	}

	public void update(String name, String description, Money price, int estimatedMinutes, Instant now) {
		requireActive();
		this.name = requireName(name);
		this.description = normalize(description);
		this.price = requireValidPrice(price);
		this.estimatedMinutes = requirePositive(estimatedMinutes);
		this.updatedAt = now;
	}

	public void deactivate(Instant now) {
		requireActive();
		this.active = false;
		this.updatedAt = now;
	}

	public void requireActive() {
		if (!active) {
			throw new DomainException("Service " + id + " is deactivated");
		}
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new DomainException("Service name is required");
		}
		return name.trim();
	}

	private static String normalize(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}

	private static Money requireValidPrice(Money price) {
		if (price == null || price.isNegative()) {
			throw new DomainException("Service price must not be negative");
		}
		return price;
	}

	private static int requirePositive(int minutes) {
		if (minutes <= 0) {
			throw new DomainException("Estimated minutes must be positive");
		}
		return minutes;
	}

	public UUID id() {
		return id;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public Money price() {
		return price;
	}

	public int estimatedMinutes() {
		return estimatedMinutes;
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

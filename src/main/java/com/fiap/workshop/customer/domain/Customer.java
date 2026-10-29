package com.fiap.workshop.customer.domain;

import com.fiap.workshop.shared.domain.Document;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Email;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;
import java.util.UUID;

public class Customer {

	private final UUID id;
	private final Document document;
	private final Instant createdAt;
	private String name;
	private Email email;
	private String phone;
	private boolean active;
	private Instant updatedAt;

	private Customer(UUID id, String name, Document document, Email email, String phone, boolean active,
			Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.name = requireName(name);
		this.document = document;
		this.email = email;
		this.phone = normalizePhone(phone);
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static Customer register(String name, Document document, Email email, String phone, Instant now) {
		return new Customer(UuidV7.generate(), name, document, email, phone, true, now, now);
	}

	public static Customer restore(UUID id, String name, Document document, Email email, String phone, boolean active,
			Instant createdAt, Instant updatedAt) {
		return new Customer(id, name, document, email, phone, active, createdAt, updatedAt);
	}

	public void update(String name, Email email, String phone, Instant now) {
		requireActive();
		this.name = requireName(name);
		this.email = email;
		this.phone = normalizePhone(phone);
		this.updatedAt = now;
	}

	public void deactivate(Instant now) {
		requireActive();
		this.active = false;
		this.updatedAt = now;
	}

	public void requireActive() {
		if (!active) {
			throw new DomainException("Customer " + id + " is deactivated");
		}
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new DomainException("Customer name is required");
		}
		return name.trim();
	}

	private static String normalizePhone(String phone) {
		return phone == null || phone.isBlank() ? null : phone.trim();
	}

	public UUID id() {
		return id;
	}

	public String name() {
		return name;
	}

	public Document document() {
		return document;
	}

	public Email email() {
		return email;
	}

	public String phone() {
		return phone;
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

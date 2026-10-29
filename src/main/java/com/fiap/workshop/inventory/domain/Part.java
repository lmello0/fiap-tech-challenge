package com.fiap.workshop.inventory.domain;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;
import java.util.UUID;

public class Part {

	private final UUID id;
	private final String sku;
	private final Instant createdAt;
	private String name;
	private String description;
	private Money unitPrice;
	private int minimumQuantity;
	private int onHand;
	private int reserved;
	private boolean active;
	private Instant updatedAt;

	private Part(UUID id, String sku, String name, String description, Money unitPrice, int minimumQuantity, int onHand,
			int reserved, boolean active, Instant createdAt, Instant updatedAt) {
		this.id = id;
		this.sku = requireSku(sku);
		this.name = requireName(name);
		this.description = normalize(description);
		this.unitPrice = requirePrice(unitPrice);
		this.minimumQuantity = requireNotNegative(minimumQuantity, "Minimum quantity");
		this.onHand = requireNotNegative(onHand, "Stock");
		this.reserved = requireNotNegative(reserved, "Reserved stock");
		this.active = active;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static Part create(String sku, String name, String description, Money unitPrice, int minimumQuantity, Instant now) {
		return new Part(UuidV7.generate(), sku, name, description, unitPrice, minimumQuantity, 0, 0, true, now, now);
	}

	public static Part restore(UUID id, String sku, String name, String description, Money unitPrice, int minimumQuantity,
			int onHand, int reserved, boolean active, Instant createdAt, Instant updatedAt) {
		return new Part(id, sku, name, description, unitPrice, minimumQuantity, onHand, reserved, active, createdAt, updatedAt);
	}

	public void update(String name, String description, Money unitPrice, int minimumQuantity, Instant now) {
		requireActive();
		this.name = requireName(name);
		this.description = normalize(description);
		this.unitPrice = requirePrice(unitPrice);
		this.minimumQuantity = requireNotNegative(minimumQuantity, "Minimum quantity");
		this.updatedAt = now;
	}

	public StockMovement receive(int quantity, String note, String performedBy, Instant now) {
		if (quantity <= 0) {
			throw new DomainException("Received quantity must be positive");
		}
		return move(MovementType.ENTRY, quantity, note, performedBy, now);
	}

	public StockMovement adjust(int delta, String reason, String performedBy, Instant now) {
		if (delta == 0) {
			throw new DomainException("Adjustment must not be zero");
		}
		if (reason == null || reason.isBlank()) {
			throw new DomainException("An adjustment requires a reason");
		}
		return move(MovementType.ADJUSTMENT, delta, reason, performedBy, now);
	}

	public void reserve(int quantity, Instant now) {
		requirePositive(quantity);
		if (available() < quantity) {
			throw new DomainException("Insufficient available stock: " + available() + " available, " + quantity + " requested");
		}
		reserved += quantity;
		updatedAt = now;
	}

	public void release(int quantity, Instant now) {
		requirePositive(quantity);
		if (reserved < quantity) {
			throw new DomainException("Cannot release " + quantity + ", only " + reserved + " reserved");
		}
		reserved -= quantity;
		updatedAt = now;
	}

	public StockMovement consume(int quantity, String reason, String performedBy, Instant now) {
		requirePositive(quantity);
		if (reserved < quantity) {
			throw new DomainException("Cannot consume " + quantity + ", only " + reserved + " reserved");
		}
		reserved -= quantity;
		onHand -= quantity;
		updatedAt = now;
		return StockMovement.record(id, MovementType.CONSUMPTION, -quantity, onHand, reason, performedBy, now);
	}

	public int available() {
		return onHand - reserved;
	}

	public boolean crossedLowStockFrom(int previousAvailable) {
		return previousAvailable > minimumQuantity && available() <= minimumQuantity;
	}

	public boolean lowStock() {
		return available() <= minimumQuantity;
	}

	public void deactivate(Instant now) {
		requireActive();
		this.active = false;
		this.updatedAt = now;
	}

	public void requireActive() {
		if (!active) {
			throw new DomainException("Part " + id + " is deactivated");
		}
	}

	private StockMovement move(MovementType type, int delta, String reason, String performedBy, Instant now) {
		requireActive();
		if (available() + delta < 0) {
			throw new DomainException("Insufficient available stock: " + available() + " available, change of " + delta);
		}
		onHand += delta;
		updatedAt = now;
		return StockMovement.record(id, type, delta, onHand, reason, performedBy, now);
	}

	private static String requireSku(String sku) {
		if (sku == null || sku.isBlank()) {
			throw new DomainException("Part SKU is required");
		}
		return sku.trim().toUpperCase();
	}

	private static String requireName(String name) {
		if (name == null || name.isBlank()) {
			throw new DomainException("Part name is required");
		}
		return name.trim();
	}

	private static String normalize(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}

	private static Money requirePrice(Money price) {
		if (price == null || price.isNegative()) {
			throw new DomainException("Part price must not be negative");
		}
		return price;
	}

	private static void requirePositive(int quantity) {
		if (quantity <= 0) {
			throw new DomainException("Quantity must be positive");
		}
	}

	private static int requireNotNegative(int value, String field) {
		if (value < 0) {
			throw new DomainException(field + " must not be negative");
		}
		return value;
	}

	public UUID id() {
		return id;
	}

	public String sku() {
		return sku;
	}

	public String name() {
		return name;
	}

	public String description() {
		return description;
	}

	public Money unitPrice() {
		return unitPrice;
	}

	public int minimumQuantity() {
		return minimumQuantity;
	}

	public int onHand() {
		return onHand;
	}

	public int reserved() {
		return reserved;
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

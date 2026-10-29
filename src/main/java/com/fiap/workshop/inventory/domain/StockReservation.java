package com.fiap.workshop.inventory.domain;

import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class StockReservation {

	private final UUID id;
	private final UUID workOrderId;
	private final String workOrderNumber;
	private final UUID budgetId;
	private final List<ReservationLine> lines;
	private final Instant createdAt;
	private ReservationStatus status;
	private Instant updatedAt;

	private StockReservation(UUID id, UUID workOrderId, String workOrderNumber, UUID budgetId, List<ReservationLine> lines,
			ReservationStatus status, Instant createdAt, Instant updatedAt) {
		if (lines.isEmpty()) {
			throw new DomainException("A reservation needs at least one part");
		}
		this.id = id;
		this.workOrderId = workOrderId;
		this.workOrderNumber = workOrderNumber;
		this.budgetId = budgetId;
		this.lines = List.copyOf(lines);
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
	}

	public static StockReservation request(UUID workOrderId, String workOrderNumber, UUID budgetId, List<ReservationLine> lines,
			Instant now) {
		return new StockReservation(UuidV7.generate(), workOrderId, workOrderNumber, budgetId, lines, ReservationStatus.WAITING, now, now);
	}

	public static StockReservation restore(UUID id, UUID workOrderId, String workOrderNumber, UUID budgetId,
			List<ReservationLine> lines, ReservationStatus status, Instant createdAt, Instant updatedAt) {
		return new StockReservation(id, workOrderId, workOrderNumber, budgetId, lines, status, createdAt, updatedAt);
	}

	public void markReserved(Instant now) {
		require(ReservationStatus.WAITING, ReservationStatus.RESERVED);
		change(ReservationStatus.RESERVED, now);
	}

	public void markConsumed(Instant now) {
		require(ReservationStatus.RESERVED, ReservationStatus.CONSUMED);
		change(ReservationStatus.CONSUMED, now);
	}

	public boolean release(Instant now) {
		if (status != ReservationStatus.WAITING && status != ReservationStatus.RESERVED) {
			throw new DomainException("A " + status + " reservation cannot be released");
		}
		boolean heldStock = status == ReservationStatus.RESERVED;
		change(ReservationStatus.RELEASED, now);
		return heldStock;
	}

	public boolean holdsStock() {
		return status == ReservationStatus.RESERVED;
	}

	private void require(ReservationStatus expected, ReservationStatus target) {
		if (status != expected) {
			throw new DomainException("Cannot move a " + status + " reservation to " + target);
		}
	}

	private void change(ReservationStatus target, Instant now) {
		status = target;
		updatedAt = now;
	}

	public UUID id() {
		return id;
	}

	public UUID workOrderId() {
		return workOrderId;
	}

	public String workOrderNumber() {
		return workOrderNumber;
	}

	public UUID budgetId() {
		return budgetId;
	}

	public List<ReservationLine> lines() {
		return lines;
	}

	public ReservationStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}
}

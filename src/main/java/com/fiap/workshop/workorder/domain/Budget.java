package com.fiap.workshop.workorder.domain;

import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Budget {

	private final UUID id;
	private final int sequence;
	private final BudgetKind kind;
	private final UUID previousBudgetId;
	private final List<BudgetLine> lines;
	private final Instant issuedAt;
	private BudgetStatus status;
	private Decision decision;

	private Budget(UUID id, int sequence, BudgetKind kind, UUID previousBudgetId, List<BudgetLine> lines, Instant issuedAt,
			BudgetStatus status, Decision decision) {
		this.id = id;
		this.sequence = sequence;
		this.kind = kind;
		this.previousBudgetId = previousBudgetId;
		this.lines = List.copyOf(lines);
		this.issuedAt = issuedAt;
		this.status = status;
		this.decision = decision;
	}

	static Budget issue(int sequence, BudgetKind kind, UUID previousBudgetId, List<BudgetLine> lines, Instant now) {
		return new Budget(UuidV7.generate(), sequence, kind, previousBudgetId, lines, now, BudgetStatus.PENDING, null);
	}

	public static Budget restore(UUID id, int sequence, BudgetKind kind, UUID previousBudgetId, List<BudgetLine> lines,
			Instant issuedAt, BudgetStatus status, Decision decision) {
		return new Budget(id, sequence, kind, previousBudgetId, lines, issuedAt, status, decision);
	}

	void approve(Decision decision) {
		decide(BudgetStatus.APPROVED, decision);
	}

	void reject(Decision decision) {
		decide(BudgetStatus.REJECTED, decision);
	}

	void cancel() {
		if (status == BudgetStatus.PENDING) {
			status = BudgetStatus.CANCELLED;
		}
	}

	private void decide(BudgetStatus outcome, Decision decision) {
		if (status != BudgetStatus.PENDING) {
			throw new WorkOrderStateException("Budget " + id + " was already " + status);
		}
		this.status = outcome;
		this.decision = decision;
	}

	public Money total() {
		return lines.stream().map(BudgetLine::subtotal).reduce(Money.ZERO, Money::plus);
	}

	public List<PartQuantity> parts() {
		Map<UUID, Integer> byPart = new LinkedHashMap<>();
		lines.stream().filter(l -> l.type() == ItemType.PART).forEach(l -> byPart.merge(l.referenceId(), l.quantity(), Integer::sum));
		return byPart.entrySet().stream().map(e -> new PartQuantity(e.getKey(), e.getValue())).toList();
	}

	public boolean hasParts() {
		return lines.stream().anyMatch(l -> l.type() == ItemType.PART);
	}

	public UUID id() {
		return id;
	}

	public int sequence() {
		return sequence;
	}

	public BudgetKind kind() {
		return kind;
	}

	public UUID previousBudgetId() {
		return previousBudgetId;
	}

	public List<BudgetLine> lines() {
		return lines;
	}

	public Instant issuedAt() {
		return issuedAt;
	}

	public BudgetStatus status() {
		return status;
	}

	public Decision decision() {
		return decision;
	}
}

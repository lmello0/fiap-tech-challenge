package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.domain.BudgetKind;
import com.fiap.workshop.workorder.domain.BudgetStatus;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "budget", schema = "workorder")
@Getter
@Setter
@NoArgsConstructor
class BudgetEntity {

	@Id
	private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "work_order_id")
	private WorkOrderEntity workOrder;
	private int sequence;
	@Enumerated(EnumType.STRING)
	private BudgetKind kind;
	@Enumerated(EnumType.STRING)
	private BudgetStatus status;
	@Column(name = "previous_budget_id")
	private UUID previousBudgetId;
	private BigDecimal total;
	@Column(name = "issued_at")
	private Instant issuedAt;
	@Column(name = "decided_at")
	private Instant decidedAt;
	@Column(name = "decided_by")
	private String decidedBy;
	@Column(name = "decided_role")
	private String decidedRole;
	@Enumerated(EnumType.STRING)
	@Column(name = "decision_channel")
	private DecisionChannel decisionChannel;
	@Column(name = "decision_reason")
	private String decisionReason;

	@OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<BudgetLineEntity> lines = new ArrayList<>();
}

package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.domain.ItemType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "budget_line", schema = "workorder")
@Getter
@Setter
@NoArgsConstructor
class BudgetLineEntity {

	@Id
	private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "budget_id")
	private BudgetEntity budget;
	private int position;
	@Enumerated(EnumType.STRING)
	private ItemType type;
	@Column(name = "reference_id")
	private UUID referenceId;
	private String name;
	@Column(name = "unit_price")
	private BigDecimal unitPrice;
	private int quantity;
}

package com.fiap.workshop.inventory.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_movement", schema = "inventory")
@Getter
@Setter
@NoArgsConstructor
class StockMovementEntity {

	@Id
	private UUID id;
	@Column(name = "part_id")
	private UUID partId;
	@Enumerated(EnumType.STRING)
	private com.fiap.workshop.inventory.domain.MovementType type;
	private int quantity;
	@Column(name = "balance_after")
	private int balanceAfter;
	private String reason;
	@Column(name = "performed_by")
	private String performedBy;
	@Column(name = "created_at")
	private Instant createdAt;
}

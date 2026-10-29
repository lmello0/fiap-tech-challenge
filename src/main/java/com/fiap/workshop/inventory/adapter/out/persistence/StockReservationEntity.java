package com.fiap.workshop.inventory.adapter.out.persistence;

import com.fiap.workshop.inventory.domain.ReservationStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "stock_reservation", schema = "inventory")
@Getter
@Setter
@NoArgsConstructor
class StockReservationEntity {

	@Id
	private UUID id;
	@Column(name = "work_order_id")
	private UUID workOrderId;
	@Column(name = "work_order_number")
	private String workOrderNumber;
	@Column(name = "budget_id")
	private UUID budgetId;
	@Enumerated(EnumType.STRING)
	private ReservationStatus status;
	@Column(name = "created_at")
	private Instant createdAt;
	@Column(name = "updated_at")
	private Instant updatedAt;
	@Version
	private long version;

	@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<StockReservationLineEntity> lines = new ArrayList<>();
}

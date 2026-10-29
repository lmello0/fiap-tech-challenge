package com.fiap.workshop.inventory.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "part", schema = "inventory")
@Getter
@Setter
@NoArgsConstructor
class PartEntity {

	@Id
	private UUID id;
	private String sku;
	private String name;
	private String description;
	@Column(name = "unit_price")
	private BigDecimal unitPrice;
	@Column(name = "minimum_quantity")
	private int minimumQuantity;
	@Column(name = "on_hand")
	private int onHand;
	private boolean active;
	@Column(name = "created_at")
	private Instant createdAt;
	@Column(name = "updated_at")
	private Instant updatedAt;
	@Version
	private long version;
}

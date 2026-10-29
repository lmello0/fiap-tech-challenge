package com.fiap.workshop.catalog.adapter.out.persistence;

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
@Table(name = "repair_service", schema = "catalog")
@Getter
@Setter
@NoArgsConstructor
class RepairServiceEntity {

	@Id
	private UUID id;
	private String name;
	private String description;
	private BigDecimal price;
	@Column(name = "estimated_minutes")
	private int estimatedMinutes;
	private boolean active;
	@Column(name = "created_at")
	private Instant createdAt;
	@Column(name = "updated_at")
	private Instant updatedAt;
	@Version
	private long version;
}

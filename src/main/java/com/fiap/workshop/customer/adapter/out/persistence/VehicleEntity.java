package com.fiap.workshop.customer.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehicle", schema = "customer")
@Getter
@Setter
@NoArgsConstructor
class VehicleEntity {

	@Id
	private UUID id;
	@Column(name = "customer_id")
	private UUID customerId;
	private String plate;
	private String brand;
	private String model;
	@Column(name = "model_year")
	private int year;
	private boolean active;
	@Column(name = "created_at")
	private Instant createdAt;
	@Column(name = "updated_at")
	private Instant updatedAt;
	@Version
	private long version;
}

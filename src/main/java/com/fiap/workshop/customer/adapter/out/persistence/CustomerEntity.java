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
@Table(name = "customer", schema = "customer")
@Getter
@Setter
@NoArgsConstructor
class CustomerEntity {

	@Id
	private UUID id;
	private String name;
	private String document;
	private String email;
	private String phone;
	private boolean active;
	@Column(name = "created_at")
	private Instant createdAt;
	@Column(name = "updated_at")
	private Instant updatedAt;
	@Version
	private long version;
}

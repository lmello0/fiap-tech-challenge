package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
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
@Table(name = "work_order", schema = "workorder")
@Getter
@Setter
@NoArgsConstructor
class WorkOrderEntity {

	@Id
	private UUID id;
	private String number;
	@Enumerated(EnumType.STRING)
	private WorkOrderStatus status;
	@Column(name = "customer_id")
	private UUID customerId;
	@Column(name = "customer_name")
	private String customerName;
	@Column(name = "customer_document")
	private String customerDocument;
	@Column(name = "customer_email")
	private String customerEmail;
	@Column(name = "vehicle_id")
	private UUID vehicleId;
	@Column(name = "vehicle_plate")
	private String vehiclePlate;
	@Column(name = "vehicle_brand")
	private String vehicleBrand;
	@Column(name = "vehicle_model")
	private String vehicleModel;
	@Column(name = "vehicle_year")
	private int vehicleYear;
	private String complaint;
	@Column(name = "created_at")
	private Instant createdAt;
	@Column(name = "updated_at")
	private Instant updatedAt;
	@Version
	private long version;

	@OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("position")
	private List<WorkOrderItemEntity> items = new ArrayList<>();

	@OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("sequence")
	private List<BudgetEntity> budgets = new ArrayList<>();

	@OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("occurredAt, id")
	private List<StatusChangeEntity> history = new ArrayList<>();
}

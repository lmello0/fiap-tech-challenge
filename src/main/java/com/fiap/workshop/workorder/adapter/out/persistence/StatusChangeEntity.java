package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "work_order_status_history", schema = "workorder")
@Getter
@Setter
@NoArgsConstructor
class StatusChangeEntity {

	@Id
	private UUID id;
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "work_order_id")
	private WorkOrderEntity workOrder;
	@Enumerated(EnumType.STRING)
	@Column(name = "from_status")
	private WorkOrderStatus fromStatus;
	@Enumerated(EnumType.STRING)
	@Column(name = "to_status")
	private WorkOrderStatus toStatus;
	@Column(name = "occurred_at")
	private Instant occurredAt;
	@Column(name = "actor_subject")
	private String actorSubject;
	@Column(name = "actor_role")
	private String actorRole;
	private String note;
}

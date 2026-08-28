package com.fiap.techchallenge.workorder.entities;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders", schema = "work_orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WorkOrder {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String orderCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private WorkOrderStatus status;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private UUID vehicleId;

    @Column(nullable = false)
    private String vehiclePlate;

    @Column(nullable = false)
    private String vehicleMake;

    @Column(nullable = false)
    private String vehicleModel;

    @Column
    private UUID assignedMechanicId;

    @Column
    private String mechanicName;

    @Column(length = 2000)
    private String customerComplaint;

    @Column(length = 2000)
    private String diagnosis;

    @Column(length = 2000)
    private String refusalReason;

    @Column(length = 2000)
    private String cancelReason;

    /** Budget owns the join (ADR 0013); "exactly one live Budget" is enforced only in the
     * application, not the database, so a future requoting flow costs no migration. */
    @OneToMany(mappedBy = "workOrder")
    private List<Budget> budgets = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant diagnosticRequestedAt;

    private Instant diagnosticStartedAt;

    private Instant diagnosticFinishedAt;

    private Instant approvedAt;

    private Instant refusedAt;

    private Instant serviceStartedAt;

    private Instant finishedAt;

    private Instant pickupReadyAt;

    private Instant deliveredAt;

    private Instant cancelledAt;

    public void addBudget(Budget budget) {
        budgets.add(budget);
        budget.setWorkOrder(this);
    }

    /** The most recently created Budget — today that's always the only one (ADR 0013). */
    public Budget getCurrentBudget() {
        return budgets.stream()
                .max(Comparator.comparing(Budget::getCreatedAt))
                .orElse(null);
    }
}

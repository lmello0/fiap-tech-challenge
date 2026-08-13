package com.fiap.techchallenge.workorder.entities;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
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
    private UUID vehicleId;

    @Column
    private UUID assignedMechanicId;

    @Column(length = 2000)
    private String customerComplaint;

    @Column(length = 2000)
    private String diagnosis;

    @Column(length = 2000)
    private String refusalReason;

    @OneToMany(
            mappedBy = "workOrder",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private List<WorkOrderRow> rows = new ArrayList<>();

    private BigDecimal laborTotal;

    private BigDecimal partsTotal;

    private BigDecimal grandTotal;

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

    public void addRow(WorkOrderRow row) {
        rows.add(row);
        row.setWorkOrder(this);
    }

    public void clearRows() {
        rows.forEach(r -> r.setWorkOrder(null));
        rows.clear();
    }
}

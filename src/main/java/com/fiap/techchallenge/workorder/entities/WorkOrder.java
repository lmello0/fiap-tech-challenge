package com.fiap.techchallenge.workorder.entities;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;
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

    @Column(nullable = false, length = 36)
    private UUID customerId;

    @Column(nullable = false, length = 36)
    private UUID vehicleId;

    @Column(nullable = false, length = 36)
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

    private BigDecimal discount;

    private BigDecimal taxTotal;

    private BigDecimal grandTotal;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    private Instant approvedAt;

    private Instant refusedAt;

    private Instant finishedAt;

    private Instant deliveredAt;
}

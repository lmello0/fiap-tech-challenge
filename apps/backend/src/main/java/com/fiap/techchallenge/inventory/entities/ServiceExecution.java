package com.fiap.techchallenge.inventory.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.UUID;

/**
 * One measured sample of how long a {@link RepairService} actually took on a work order — the raw
 * data {@link RepairService#getAverageSeconds()} is rolled up from. {@code workOrderRowId} is unique:
 * a SERVICE row can be started and finished exactly once, so it can only ever produce one sample.
 */
@Entity
@Table(
        name = "service_executions",
        schema = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_service_executions_work_order_row_id", columnNames = "work_order_row_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceExecution {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private RepairService repairService;

    @Column(nullable = false)
    private UUID workOrderId;

    @Column(nullable = false)
    private UUID workOrderRowId;

    @Column(nullable = false)
    private Integer durationSeconds;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant recordedAt;
}

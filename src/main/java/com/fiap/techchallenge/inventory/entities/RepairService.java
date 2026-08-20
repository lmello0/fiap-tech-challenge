package com.fiap.techchallenge.inventory.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The catalog entry for a nameable unit of work the shop sells ("brake pad replacement"). Named
 * {@code RepairService} rather than {@code Service} — the domain term is "Service", but that name
 * collides with {@code org.springframework.stereotype.Service} in every class that needs to import
 * both.
 */
@Entity
@Table(
        name = "repair_services",
        schema = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_repair_services_code", columnNames = "code")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class RepairService {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer estimatedSeconds;

    /** {@code null} until the first execution sample is recorded. */
    private Integer averageSeconds;

    @Column(nullable = false)
    private int executionCount = 0;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * The duration to quote for this service: the measured rolling average once any executions have
     * been recorded, otherwise the seeded estimate.
     */
    public int getEffectiveAverageSeconds() {
        return averageSeconds != null ? averageSeconds : estimatedSeconds;
    }

    public void deactivate() {
        this.active = false;
    }
}

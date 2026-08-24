package com.fiap.techchallenge.inventory.entities;

import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
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
 * A physical item held in stock and sold on a work order. Purely a catalog row — on-hand, reserved,
 * available, and cost are never stored here; they are derived from {@link StockMovement} and
 * {@link PartReservation} (see the {@code inventory.part_stock} view). The row itself is still the
 * locking point for a reserve/consume/receive/adjust critical section: {@code findByIdForUpdate}
 * takes a pessimistic write lock on it before any of those touch the ledger, even though the row's own
 * columns never change as a result.
 */
@Entity
@Table(
        name = "parts",
        schema = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_parts_sku", columnNames = "sku")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Part {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(length = 100)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UnitOfMeasure unitOfMeasure;

    @Column(nullable = false)
    private BigDecimal salePrice;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public void deactivate() {
        this.active = false;
    }
}

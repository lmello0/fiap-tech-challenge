package com.fiap.techchallenge.inventory.entities;

import com.fiap.techchallenge.inventory.enums.StockMovementType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * The only record of a {@link Part}'s stock: on-hand is {@code SUM(quantity)} over a part's
 * movements, nothing else stores it. Reservations are deliberately not movements — they claim stock
 * without moving it. Each type carries exactly the source reference that explains it: {@code
 * purchaseOrderLine} for a {@code PURCHASE}, {@code workOrderId} for a {@code CONSUMPTION}, {@code
 * reason} for an {@code ADJUSTMENT} (enforced by {@code chk_movement_source_matches_type} in the
 * schema).
 */
@Entity
@Table(name = "stock_movements", schema = "inventory")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StockMovement {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Part part;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType type;

    /** Signed: positive for a purchase or an upward adjustment, negative for consumption or a downward adjustment. */
    @Column(nullable = false)
    private BigDecimal quantity;

    /** Required for {@link StockMovementType#PURCHASE}; null otherwise. */
    private BigDecimal unitCost;

    /** Required for {@link StockMovementType#ADJUSTMENT}; null otherwise. */
    @Column(length = 500)
    private String reason;

    /** Required for {@link StockMovementType#CONSUMPTION}; null otherwise. */
    private UUID workOrderId;

    /** Required for {@link StockMovementType#PURCHASE}; null otherwise. */
    @ManyToOne
    private PurchaseOrderLine purchaseOrderLine;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;
}

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
 * An append-only record of a real change in a {@link Part}'s on-hand quantity. Reservations are
 * deliberately not movements — they claim stock without moving it.
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

    private BigDecimal unitCost;

    /** The purchase order or work order this movement traces back to, when it has one. */
    private UUID referenceId;

    /** Required for {@link StockMovementType#ADJUSTMENT}; optional otherwise (see {@code chk_adjustment_has_reason}). */
    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant occurredAt;
}

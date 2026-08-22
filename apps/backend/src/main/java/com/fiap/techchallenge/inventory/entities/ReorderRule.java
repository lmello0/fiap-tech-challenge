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
 * A standing min/max (order-up-to) instruction for one part: when its inventory position falls to or
 * below {@code minQuantity}, order enough from {@code vendor} to bring it back up to
 * {@code maxQuantity}. A part with no row here is one nobody has decided to auto-stock; {@code
 * enabled = false} is a decision someone made on a part that otherwise has one.
 */
@Entity
@Table(
        name = "reorder_rules",
        schema = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_reorder_rules_part_id", columnNames = "part_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ReorderRule {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Part part;

    @Column(nullable = false)
    private BigDecimal minQuantity;

    @Column(nullable = false)
    private BigDecimal maxQuantity;

    @ManyToOne(optional = false)
    private Vendor vendor;

    @Column(nullable = false)
    private boolean enabled = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * How much to place with the vendor to bring inventory position back up to {@code maxQuantity}.
     * Only meaningful when {@code position <= minQuantity}; the {@code max > min} table constraint
     * guarantees this is always positive at that point.
     */
    public BigDecimal quantityToOrder(BigDecimal position) {
        return maxQuantity.subtract(position);
    }
}

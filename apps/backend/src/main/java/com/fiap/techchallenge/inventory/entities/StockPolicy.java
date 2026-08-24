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
 * A standing low-stock threshold for one part, and optionally the order-up-to policy the shop wants
 * followed when it's crossed. {@code minQuantity} always defines "low" for this part. {@code
 * maxQuantity} and {@code vendor} are only meaningful — and only required — when {@code
 * autoReorderEnabled} is true; a part restocked by hand can still have its low threshold tracked
 * without ever placing an automatic purchase order. A part with no row here is one nobody has decided
 * to track at all.
 */
@Entity
@Table(
        name = "stock_policies",
        schema = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_stock_policies_part_id", columnNames = "part_id")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class StockPolicy {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Part part;

    @Column(nullable = false)
    private BigDecimal minQuantity;

    private BigDecimal maxQuantity;

    @ManyToOne
    private Vendor vendor;

    @Column(nullable = false)
    private boolean autoReorderEnabled = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * How much to place with the vendor to bring inventory position back up to {@code maxQuantity}.
     * Only meaningful when {@code autoReorderEnabled} and {@code position <= minQuantity}; the
     * {@code max > min} table constraint guarantees this is always positive at that point.
     */
    public BigDecimal quantityToOrder(BigDecimal position) {
        return maxQuantity.subtract(position);
    }
}

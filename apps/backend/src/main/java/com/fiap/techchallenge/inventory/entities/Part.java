package com.fiap.techchallenge.inventory.entities;

import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import com.fiap.techchallenge.inventory.exceptions.InvalidStockAdjustmentException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

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
    private BigDecimal averageCost = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal quantityOnHand = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal quantityReserved = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    /**
     * What can still be promised to a new reservation: physically present, minus what earlier
     * reservations already claim.
     */
    public BigDecimal getAvailable() {
        return quantityOnHand.subtract(quantityReserved);
    }

    public void deactivate() {
        this.active = false;
    }

    /**
     * Applies a manual correction to {@code quantityOnHand}. {@code delta} may be positive (found
     * stock) or negative (breakage, loss, miscount) but can never drop on-hand below what is already
     * reserved for a work order — that reservation is a promise this part cannot break.
     */
    public void applyAdjustment(BigDecimal delta) {
        BigDecimal newOnHand = quantityOnHand.add(delta);

        if (newOnHand.signum() < 0) {
            throw new InvalidStockAdjustmentException(
                    "Adjustment would drop on-hand quantity below zero for part " + id);
        }

        if (newOnHand.compareTo(quantityReserved) < 0) {
            throw new InvalidStockAdjustmentException(
                    "Adjustment would drop on-hand quantity below what is already reserved for part " + id);
        }

        this.quantityOnHand = newOnHand;
    }

    /**
     * Claims up to {@code quantity} of what is currently available. Returns the amount actually
     * reserved, which may be less than {@code quantity} — reserving is best-effort, so the caller is
     * expected to record the gap as a shortfall rather than treat this as a failure.
     */
    public BigDecimal reserve(BigDecimal quantity) {
        BigDecimal toReserve = quantity.min(getAvailable());

        if (toReserve.signum() > 0) {
            this.quantityReserved = quantityReserved.add(toReserve);
        }

        return toReserve;
    }

    /**
     * Returns a previously reserved quantity to availability without touching {@code quantityOnHand}
     * — nothing physical moved, so no {@link StockMovement} is written for this.
     */
    public void releaseReservation(BigDecimal quantity) {
        BigDecimal newReserved = quantityReserved.subtract(quantity);

        if (newReserved.signum() < 0) {
            throw new InvalidStockAdjustmentException(
                    "Cannot release more than what is reserved for part " + id);
        }

        this.quantityReserved = newReserved;
    }

    /**
     * Writes off a previously reserved quantity: it stops being reserved and stops being on hand in
     * the same step, since consuming is when a reservation's claim becomes a real, physical removal.
     */
    public void consumeReservation(BigDecimal quantity) {
        releaseReservation(quantity);
        this.quantityOnHand = quantityOnHand.subtract(quantity);
    }

    /**
     * Records newly received stock and rolls it into {@code averageCost}: the weighted average of
     * what was already on hand at its old average cost, and what just arrived at {@code unitCost}.
     */
    public void receive(BigDecimal quantity, BigDecimal unitCost) {
        BigDecimal previousCostTotal = quantityOnHand.multiply(averageCost);
        BigDecimal receivedCostTotal = quantity.multiply(unitCost);
        BigDecimal newOnHand = quantityOnHand.add(quantity);

        this.averageCost = newOnHand.signum() == 0
                ? BigDecimal.ZERO
                : previousCostTotal.add(receivedCostTotal).divide(newOnHand, 2, RoundingMode.HALF_UP);
        this.quantityOnHand = newOnHand;
    }
}

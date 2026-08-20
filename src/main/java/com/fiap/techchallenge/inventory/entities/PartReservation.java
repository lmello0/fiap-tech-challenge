package com.fiap.techchallenge.inventory.entities;

import com.fiap.techchallenge.inventory.enums.ReservationStatus;
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
 * A work order's claim on a quantity of a {@link Part}, held between quoting and starting service.
 * Reserving is best-effort: {@code quantityReserved} may be less than {@code quantityRequested} when
 * stock ran short at quote time, and the gap is the reservation's shortfall. Every reservation belongs
 * to exactly one work order — there is no way to reserve a part outside of one.
 */
@Entity
@Table(name = "part_reservations", schema = "inventory")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PartReservation {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Part part;

    @Column(nullable = false)
    private UUID workOrderId;

    @Column(nullable = false)
    private BigDecimal quantityRequested;

    @Column(nullable = false)
    private BigDecimal quantityReserved;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant reservedAt;

    private Instant resolvedAt;

    public BigDecimal getShortfall() {
        return quantityRequested.subtract(quantityReserved);
    }

    public boolean hasShortfall() {
        return getShortfall().signum() > 0;
    }
}

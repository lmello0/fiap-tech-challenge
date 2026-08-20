package com.fiap.techchallenge.inventory.entities;

import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "purchase_orders",
        schema = "inventory",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_purchase_orders_code", columnNames = "code")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PurchaseOrder {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 30)
    private String code;

    @ManyToOne(optional = false)
    private Vendor vendor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PurchaseOrderStatus status;

    @Column(length = 100)
    private String vendorOrderRef;

    @Column(nullable = false)
    private Instant placedAt;

    private Instant expectedAt;

    private Instant receivedAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    public void addLine(PurchaseOrderLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
    }
}

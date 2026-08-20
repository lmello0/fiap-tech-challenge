package com.fiap.techchallenge.inventory.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "purchase_order_lines", schema = "inventory")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class PurchaseOrderLine {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private PurchaseOrder purchaseOrder;

    @ManyToOne(optional = false)
    private Part part;

    @Column(nullable = false)
    private BigDecimal quantityOrdered;

    @Column(nullable = false)
    private BigDecimal quantityReceived = BigDecimal.ZERO;

    /** The cost paid per unit on the most recent receipt against this line; null until first receipt. */
    private BigDecimal unitCost;
}

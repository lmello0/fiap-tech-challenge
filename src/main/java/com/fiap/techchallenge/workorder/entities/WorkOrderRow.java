package com.fiap.techchallenge.workorder.entities;

import com.fiap.techchallenge.workorder.enums.RowType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_rows", schema = "work_orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class WorkOrderRow {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RowType type;

    @Column(length = 2000)
    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private UUID partId;

    public BigDecimal getTotal() {
        return quantity.multiply(unitPrice);
    }
}

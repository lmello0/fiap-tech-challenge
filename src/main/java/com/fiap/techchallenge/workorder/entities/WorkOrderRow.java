package com.fiap.techchallenge.workorder.entities;

import com.fiap.techchallenge.workorder.enums.RowType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "order_rows", schema = "work_orders")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(nullable = false, updatable = false)
    private BigDecimal lineTotal;

    private UUID partId;
}

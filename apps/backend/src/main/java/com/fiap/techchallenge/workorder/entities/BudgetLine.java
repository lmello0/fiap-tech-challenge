package com.fiap.techchallenge.workorder.entities;

import com.fiap.techchallenge.workorder.enums.RowType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "budget_lines", schema = "work_orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class BudgetLine {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(optional = false)
    private Budget budget;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RowType type;

    @Column(length = 2000)
    private String description;

    private BigDecimal quantity;

    private BigDecimal unitPrice;

    private UUID partId;

    private UUID serviceId;

    /** Only ever set on a SERVICE line, and only while the work order is IN_PROGRESS. */
    private Instant startedAt;

    private Instant finishedAt;

    public BigDecimal getTotal() {
        return quantity.multiply(unitPrice);
    }
}

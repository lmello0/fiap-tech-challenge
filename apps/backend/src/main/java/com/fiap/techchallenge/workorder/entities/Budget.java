package com.fiap.techchallenge.workorder.entities;

import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A Budget is a separate entity from its WorkOrder (ADR 0008) — the WorkOrder holds no items of its
 * own, only a reference to its current Budget. Send/approve/refuse lifecycle is decoupled from the
 * WorkOrder's own status (ADR 0009); draft line edits reserve/release only their delta (ADR 0010).
 */
@Entity
@Table(name = "budgets", schema = "work_orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Budget {

    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.VERSION_7)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "work_order_id", nullable = false, updatable = false)
    private WorkOrder workOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private BudgetStatus status;

    @OneToMany(mappedBy = "budget", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BudgetLine> lines = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant sentAt;

    private Instant resolvedAt;

    public UUID getWorkOrderId() {
        return workOrder != null ? workOrder.getId() : null;
    }

    public void addLine(BudgetLine line) {
        lines.add(line);
        line.setBudget(this);
    }

    public void removeLine(BudgetLine line) {
        lines.remove(line);
        line.setBudget(null);
    }

    public boolean isLocked() {
        return status != BudgetStatus.DRAFT;
    }

    /** Money, R$, always 2 decimal places — the one total a customer is asked to approve must read
     * the same everywhere, not accumulate whatever scale its line prices happened to carry. */
    public BigDecimal getGrandTotal() {
        return lines.stream()
                .map(BudgetLine::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}

package com.fiap.techchallenge.inventory.entities;

import com.fiap.techchallenge.inventory.enums.StockStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Read-only projection of the {@code inventory.part_stock} view: everything about a part's stock
 * that is derived rather than stored — on-hand and available (from {@link StockMovement} and
 * {@link PartReservation}), {@link StockStatus} against its {@link StockPolicy}, and moving-average
 * purchase cost over four fixed windows. Never written to directly; the view recomputes it from the
 * ledger on every read.
 */
@Entity
@Immutable
@Table(name = "vw_part_stock", schema = "inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PartStock {

    @Id
    @Column(name = "part_id")
    private UUID partId;

    @Column(name = "on_hand")
    private BigDecimal onHand;

    private BigDecimal reserved;

    private BigDecimal available;

    @Enumerated(EnumType.STRING)
    @Column(name = "stock_status", length = 20)
    private StockStatus stockStatus;

    @Column(name = "avg_cost_30d")
    private BigDecimal avgCost30d;

    @Column(name = "avg_cost_90d")
    private BigDecimal avgCost90d;

    @Column(name = "avg_cost_365d")
    private BigDecimal avgCost365d;

    @Column(name = "avg_cost_all_time")
    private BigDecimal avgCostAllTime;
}

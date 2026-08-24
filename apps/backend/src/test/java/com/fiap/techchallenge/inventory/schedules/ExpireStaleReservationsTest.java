package com.fiap.techchallenge.inventory.schedules;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.ReservePartCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.PartStockInfo;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code app.inventory.reservation-ttl} is 7 days (application.yaml) — a reservation older than that
 * is assumed abandoned (its work order never started service) and is released back to availability.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ExpireStaleReservationsTest {

    @Autowired
    ExpireStaleReservations job;

    @Autowired
    PartReservationService reservationService;

    @Autowired
    PartCatalogService partCatalogService;

    @Autowired
    StockService stockService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void releasesReservationsOlderThanTheRetentionWindowAndLeavesRecentOnesAlone() {
        PartInfo part = createPart("EXPIRE-1");
        stockService.adjust(part.id(), new AdjustStockCommand(BigDecimal.valueOf(15), "Seed"));

        UUID staleWorkOrder = UUID.randomUUID();
        UUID freshWorkOrder = UUID.randomUUID();

        reservationService.reserveForWorkOrder(staleWorkOrder, List.of(new ReservePartCommand(part.id(), BigDecimal.TEN)));
        reservationService.reserveForWorkOrder(freshWorkOrder, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(5))));

        backdateReservation(staleWorkOrder, Instant.now().minus(8, ChronoUnit.DAYS));

        int expired = job.runExpiry();

        assertThat(expired).isEqualTo(1);

        // The stale (10-unit) reservation is released back to availability; the fresh (5-unit) one
        // keeps its claim.
        PartStockInfo afterExpiry = stockService.getStock(part.id());
        assertThat(afterExpiry.reserved()).isEqualByComparingTo("5");
    }

    private void backdateReservation(UUID workOrderId, Instant reservedAt) {
        int updated = jdbcTemplate.update(
                "UPDATE inventory.part_reservations SET reserved_at = ? WHERE work_order_id = ?",
                java.sql.Timestamp.from(reservedAt), workOrderId);

        assertThat(updated).isEqualTo(1);
    }

    private PartInfo createPart(String sku) {
        return partCatalogService.create(new CreatePartCommand(
                sku, "Test Part " + sku, null, null, UnitOfMeasure.UNIT, BigDecimal.valueOf(50)));
    }
}

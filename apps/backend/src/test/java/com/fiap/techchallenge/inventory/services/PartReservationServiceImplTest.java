package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.ReservePartCommand;
import com.fiap.techchallenge.inventory.api.events.PartReservationExpiredEvent;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.PartStockInfo;
import com.fiap.techchallenge.inventory.entities.PartReservation;
import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import com.fiap.techchallenge.inventory.repositories.PartReservationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code releasePartial} backs a budget draft line's quantity being reduced (a full line removal goes
 * through {@code releaseForWorkOrder} instead, already covered by
 * {@code workorder.PartReservationFlowTest}) — see the Inventory context's "Reservation" glossary
 * entry and ADR 0010 (draft edits reserve/release live).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@RecordApplicationEvents
class PartReservationServiceImplTest {

    @Autowired
    PartReservationService reservationService;

    @Autowired
    PartCatalogService partCatalogService;

    @Autowired
    StockService stockService;

    @Autowired
    PartReservationRepository reservationRepository;

    @Autowired
    ApplicationEvents events;

    @Test
    void releasingLessThanReservedKeepsTheReservationHeldAndFreesStock() {
        PartInfo part = createPart("RELPART-1", 5);
        UUID workOrderId = UUID.randomUUID();
        reservationService.reserveForWorkOrder(workOrderId, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(5))));

        reservationService.releasePartial(workOrderId, part.id(), BigDecimal.valueOf(2));

        PartReservation reservation = onlyReservation(workOrderId);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.HELD);
        assertThat(reservation.getQuantityReserved()).isEqualByComparingTo("3");
        assertThat(reservation.getQuantityRequested()).isEqualByComparingTo("3");
        assertThat(stockService.getStock(part.id()).available()).isEqualByComparingTo("2");
    }

    @Test
    void releasingTheFullReservedAmountResolvesTheReservationToReleased() {
        PartInfo part = createPart("RELPART-2", 5);
        UUID workOrderId = UUID.randomUUID();
        reservationService.reserveForWorkOrder(workOrderId, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(5))));

        reservationService.releasePartial(workOrderId, part.id(), BigDecimal.valueOf(5));

        PartReservation reservation = onlyReservation(workOrderId);
        assertThat(reservation.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(reservation.getQuantityReserved()).isEqualByComparingTo("0");
        assertThat(reservation.getQuantityRequested()).isEqualByComparingTo("0");
        assertThat(reservation.getResolvedAt()).isNotNull();
        assertThat(stockService.getStock(part.id()).available()).isEqualByComparingTo("5");
    }

    @Test
    void releasingAcrossTwoReservationsDrainsTheMostRecentOneFirst() {
        PartInfo part = createPart("RELPART-3", 10);
        UUID workOrderId = UUID.randomUUID();
        reservationService.reserveForWorkOrder(workOrderId, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(3))));
        reservationService.reserveForWorkOrder(workOrderId, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(3))));

        // Releases 4: fully drains the most-recently-reserved 3, then takes 1 from the older one.
        reservationService.releasePartial(workOrderId, part.id(), BigDecimal.valueOf(4));

        List<PartReservation> all = reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.HELD);
        assertThat(all).hasSize(1);
        assertThat(all.get(0).getQuantityReserved()).isEqualByComparingTo("2");

        List<PartReservation> released = reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.RELEASED);
        assertThat(released).hasSize(1);
        assertThat(released.get(0).getQuantityReserved()).isEqualByComparingTo("0");

        assertThat(stockService.getStock(part.id()).reserved()).isEqualByComparingTo("2");
    }

    @Test
    void expiringAReservationThatNeverHeldAnyStockSkipsThePartAndPublishesNoEvent() {
        // 0 on hand: reserve() caps at availability, so this reservation is pure shortfall — reserved
        // stays 0 even though requested is 5.
        PartInfo part = createPart("RELPART-4", 0);
        UUID workOrderId = UUID.randomUUID();
        reservationService.reserveForWorkOrder(workOrderId, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(5))));

        // A future cutoff matches every still-HELD reservation in the database, including ones left
        // behind by earlier tests in this class — so this only asserts on our own work order's
        // reservation and events, not the (test-order-dependent) global expired count.
        reservationService.expireStaleReservations(Instant.now().plusSeconds(60));

        assertThat(onlyReservation(workOrderId).getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        assertThat(stockService.getStock(part.id()).reserved()).isEqualByComparingTo("0");
        assertThat(events.stream(PartReservationExpiredEvent.class)
                .filter(e -> e.workOrderId().equals(workOrderId))
                .toList()).isEmpty();
    }

    private PartReservation onlyReservation(UUID workOrderId) {
        List<PartReservation> all = reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.HELD);

        if (!all.isEmpty()) {
            return all.get(0);
        }

        return reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.RELEASED).stream()
                .findFirst()
                .or(() -> reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.EXPIRED).stream().findFirst())
                .orElseThrow();
    }

    private PartInfo createPart(String sku, int onHand) {
        PartInfo part = partCatalogService.create(new CreatePartCommand(
                sku, "Test Part " + sku, null, null, UnitOfMeasure.UNIT, BigDecimal.valueOf(50)));

        if (onHand > 0) {
            stockService.adjust(part.id(), new AdjustStockCommand(BigDecimal.valueOf(onHand), "Seed"));
        }

        return partCatalogService.getById(part.id());
    }
}

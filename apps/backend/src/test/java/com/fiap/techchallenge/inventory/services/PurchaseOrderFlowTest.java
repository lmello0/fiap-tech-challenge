package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.PurchaseOrderService;
import com.fiap.techchallenge.inventory.api.VendorService;
import com.fiap.techchallenge.inventory.api.commands.*;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderLineInfo;
import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import com.fiap.techchallenge.inventory.entities.PartReservation;
import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import com.fiap.techchallenge.inventory.enums.StockMovementType;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import com.fiap.techchallenge.inventory.exceptions.IllegalPurchaseOrderStateException;
import com.fiap.techchallenge.inventory.repositories.PartReservationRepository;
import com.fiap.techchallenge.inventory.repositories.StockMovementRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises purchasing end to end against the mocked vendor client: placing an order, receiving it
 * (fully and partially), the moving-average cost recompute, and the shortfall top-up that lets a
 * customer's under-stocked quote catch up once the shop actually restocks the part.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PurchaseOrderFlowTest {

    @Autowired
    PurchaseOrderService purchaseOrderService;

    @Autowired
    VendorService vendorService;

    @Autowired
    PartCatalogService partCatalogService;

    @Autowired
    PartReservationService reservationService;

    @Autowired
    PartReservationRepository reservationRepository;

    @Autowired
    StockMovementRepository movementRepository;

    @Test
    void placingAnOrderConfirmsWithTheMockVendorAndRecordsItAsPlaced() {
        VendorInfo vendor = createVendor("Placing Vendor");
        PartInfo part = createPart("PO-PLACE-1");

        PurchaseOrderInfo po = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.TEN))));

        assertThat(po.status()).isEqualTo(PurchaseOrderStatus.PLACED);
        assertThat(po.vendorOrderRef()).isNotBlank();
        assertThat(po.expectedAt()).isNotNull();
        assertThat(po.lines()).hasSize(1);
        assertThat(po.lines().get(0).quantityOrdered()).isEqualByComparingTo("10");
    }

    @Test
    void receivingInFullUpdatesOnHandAverageCostAndMovesToReceived() {
        VendorInfo vendor = createVendor("Full Receive Vendor");
        PartInfo part = createPart("PO-FULL-1");

        PurchaseOrderInfo placed = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.TEN))));
        PurchaseOrderLineInfo line = placed.lines().get(0);

        PurchaseOrderInfo received = purchaseOrderService.receive(placed.id(), new ReceivePurchaseOrderCommand(
                List.of(new ReceivePurchaseOrderLineCommand(line.id(), BigDecimal.TEN, BigDecimal.valueOf(20)))));

        assertThat(received.status()).isEqualTo(PurchaseOrderStatus.RECEIVED);
        assertThat(received.receivedAt()).isNotNull();

        PartInfo afterReceipt = partCatalogService.getById(part.id());
        assertThat(afterReceipt.quantityOnHand()).isEqualByComparingTo("10");
        // starting averageCost is 0 (no prior stock), so the new average is exactly the receipt cost
        assertThat(afterReceipt.averageCost()).isEqualByComparingTo("20.00");

        boolean hasPurchaseMovement = movementRepository.findByPartIdOrderByOccurredAtDesc(part.id(), Pageable.unpaged())
                .stream()
                .anyMatch(m -> m.getType() == StockMovementType.PURCHASE
                        && m.getQuantity().compareTo(BigDecimal.TEN) == 0
                        && placed.id().equals(m.getReferenceId()));
        assertThat(hasPurchaseMovement).isTrue();
    }

    @Test
    void receivingPartiallyLeavesThePurchaseOrderOpen() {
        VendorInfo vendor = createVendor("Partial Vendor");
        PartInfo part = createPart("PO-PARTIAL-1");

        PurchaseOrderInfo placed = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.TEN))));
        PurchaseOrderLineInfo line = placed.lines().get(0);

        PurchaseOrderInfo partiallyReceived = purchaseOrderService.receive(placed.id(), new ReceivePurchaseOrderCommand(
                List.of(new ReceivePurchaseOrderLineCommand(line.id(), BigDecimal.valueOf(4), BigDecimal.valueOf(20)))));

        assertThat(partiallyReceived.status()).isEqualTo(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        assertThat(partiallyReceived.receivedAt()).isNull();
        assertThat(partCatalogService.getById(part.id()).quantityOnHand()).isEqualByComparingTo("4");
    }

    @Test
    void averageCostIsAWeightedMeanAcrossTwoReceiptsAtDifferentPrices() {
        VendorInfo vendor = createVendor("Weighted Vendor");
        PartInfo part = createPart("PO-WEIGHTED-1");

        // First receipt: 10 units at 10.00 -> onHand 10, avgCost 10.00
        PurchaseOrderInfo firstPo = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.TEN))));
        purchaseOrderService.receive(firstPo.id(), new ReceivePurchaseOrderCommand(
                List.of(new ReceivePurchaseOrderLineCommand(firstPo.lines().get(0).id(), BigDecimal.TEN, BigDecimal.TEN))));

        // Second receipt: 10 units at 20.00 -> onHand 20, avgCost (10*10 + 10*20)/20 = 15.00
        PurchaseOrderInfo secondPo = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.TEN))));
        purchaseOrderService.receive(secondPo.id(), new ReceivePurchaseOrderCommand(
                List.of(new ReceivePurchaseOrderLineCommand(secondPo.lines().get(0).id(), BigDecimal.TEN, BigDecimal.valueOf(20)))));

        PartInfo afterBoth = partCatalogService.getById(part.id());
        assertThat(afterBoth.quantityOnHand()).isEqualByComparingTo("20");
        assertThat(afterBoth.averageCost()).isEqualByComparingTo("15.00");
    }

    @Test
    void receivingToppsUpTheOldestShortfallFirst() {
        VendorInfo vendor = createVendor("Topup Vendor");
        PartInfo part = createPart("PO-TOPUP-1");

        // No stock at all yet: both reservations are 100% shortfall.
        UUID olderWorkOrder = UUID.randomUUID();
        UUID newerWorkOrder = UUID.randomUUID();
        reservationService.reserveForWorkOrder(olderWorkOrder, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(3))));
        reservationService.reserveForWorkOrder(newerWorkOrder, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(3))));

        // reservedAt is set by @CreationTimestamp at persist time; back-date the first reservation
        // explicitly rather than relying on the two save() calls above landing in different
        // microseconds, so the FIFO ordering this test asserts on isn't timing-dependent.
        PartReservation olderReservation =
                reservationRepository.findByWorkOrderIdAndStatus(olderWorkOrder, ReservationStatus.HELD).get(0);
        olderReservation.setReservedAt(olderReservation.getReservedAt().minusSeconds(60));
        reservationRepository.save(olderReservation);

        PurchaseOrderInfo po = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.valueOf(4)))));

        // Only enough arrives to cover the older reservation fully, with 1 left for the newer one.
        purchaseOrderService.receive(po.id(), new ReceivePurchaseOrderCommand(
                List.of(new ReceivePurchaseOrderLineCommand(po.lines().get(0).id(), BigDecimal.valueOf(4), BigDecimal.TEN))));

        PartReservation older = reservationRepository.findByWorkOrderIdAndStatus(olderWorkOrder, ReservationStatus.HELD).get(0);
        PartReservation newer = reservationRepository.findByWorkOrderIdAndStatus(newerWorkOrder, ReservationStatus.HELD).get(0);

        assertThat(older.getQuantityReserved()).isEqualByComparingTo("3");
        assertThat(older.hasShortfall()).isFalse();
        assertThat(newer.getQuantityReserved()).isEqualByComparingTo("1");
        assertThat(newer.getShortfall()).isEqualByComparingTo("2");
    }

    @Test
    void placingAgainstAnUnknownVendorFails() {
        PartInfo part = createPart("PO-NOVENDOR-1");

        assertThatThrownBy(() -> purchaseOrderService.place(new PlacePurchaseOrderCommand(
                UUID.randomUUID(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.ONE)))))
                .isInstanceOf(com.fiap.techchallenge.inventory.exceptions.VendorNotFoundException.class);
    }

    @Test
    void placingALineAgainstAnUnknownPartFails() {
        VendorInfo vendor = createVendor("No Part Vendor");

        assertThatThrownBy(() -> purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(UUID.randomUUID(), BigDecimal.ONE)))))
                .isInstanceOf(com.fiap.techchallenge.inventory.exceptions.PartNotFoundException.class);
    }

    @Test
    void receivingAnUnknownPurchaseOrderFails() {
        assertThatThrownBy(() -> purchaseOrderService.receive(UUID.randomUUID(), new ReceivePurchaseOrderCommand(List.of())))
                .isInstanceOf(com.fiap.techchallenge.inventory.exceptions.PurchaseOrderNotFoundException.class);
    }

    @Test
    void receivingALineThatDoesNotBelongToTheOrderFails() {
        VendorInfo vendor = createVendor("Wrong Line Vendor");
        PartInfo part = createPart("PO-WRONGLINE-1");

        PurchaseOrderInfo placed = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.ONE))));

        assertThatThrownBy(() -> purchaseOrderService.receive(placed.id(), new ReceivePurchaseOrderCommand(
                List.of(new ReceivePurchaseOrderLineCommand(UUID.randomUUID(), BigDecimal.ONE, BigDecimal.ONE)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to purchase order");
    }

    @Test
    void receivingStopsToppingUpFurtherReservationsOnceAvailableRunsOut() {
        VendorInfo vendor = createVendor("Break Topup Vendor");
        PartInfo part = createPart("PO-BREAK-1");

        // Three shortfall reservations, oldest first; only 3 units arrive — exactly enough for the
        // first two, leaving nothing for the third, whose loop iteration must never be reached.
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        reservationService.reserveForWorkOrder(first, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(2))));
        reservationService.reserveForWorkOrder(second, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(1))));
        reservationService.reserveForWorkOrder(third, List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(5))));

        backdate(first, 120);
        backdate(second, 60);

        PurchaseOrderInfo po = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.valueOf(3)))));
        purchaseOrderService.receive(po.id(), new ReceivePurchaseOrderCommand(
                List.of(new ReceivePurchaseOrderLineCommand(po.lines().get(0).id(), BigDecimal.valueOf(3), BigDecimal.TEN))));

        assertThat(reservationRepository.findByWorkOrderIdAndStatus(first, ReservationStatus.HELD).get(0).hasShortfall()).isFalse();
        assertThat(reservationRepository.findByWorkOrderIdAndStatus(second, ReservationStatus.HELD).get(0).hasShortfall()).isFalse();
        assertThat(reservationRepository.findByWorkOrderIdAndStatus(third, ReservationStatus.HELD).get(0).getQuantityReserved())
                .isEqualByComparingTo("0");
    }

    private void backdate(UUID workOrderId, int secondsAgo) {
        PartReservation reservation = reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.HELD).get(0);
        reservation.setReservedAt(reservation.getReservedAt().minusSeconds(secondsAgo));
        reservationRepository.save(reservation);
    }

    @Test
    void cancellingAPlacedOrderStopsItFromBeingReceived() {
        VendorInfo vendor = createVendor("Cancel Vendor");
        PartInfo part = createPart("PO-CANCEL-1");

        PurchaseOrderInfo placed = purchaseOrderService.place(new PlacePurchaseOrderCommand(
                vendor.id(), List.of(new PlacePurchaseOrderLineCommand(part.id(), BigDecimal.ONE))));

        PurchaseOrderInfo cancelled = purchaseOrderService.cancel(placed.id());
        assertThat(cancelled.status()).isEqualTo(PurchaseOrderStatus.CANCELLED);

        assertThatThrownBy(() -> purchaseOrderService.receive(placed.id(), new ReceivePurchaseOrderCommand(
                List.of(new ReceivePurchaseOrderLineCommand(placed.lines().get(0).id(), BigDecimal.ONE, BigDecimal.ONE)))))
                .isInstanceOf(IllegalPurchaseOrderStateException.class);
    }

    // --- fixtures -----------------------------------------------------------------------------

    private VendorInfo createVendor(String name) {
        return vendorService.create(new CreateVendorCommand(name, null));
    }

    private PartInfo createPart(String sku) {
        return partCatalogService.create(new CreatePartCommand(
                sku, "Test Part " + sku, null, null, UnitOfMeasure.UNIT, BigDecimal.valueOf(50)));
    }
}

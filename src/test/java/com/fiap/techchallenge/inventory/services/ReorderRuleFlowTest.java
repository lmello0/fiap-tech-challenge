package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.PurchaseOrderService;
import com.fiap.techchallenge.inventory.api.ReorderRuleService;
import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.VendorService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.CreateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.commands.CreateVendorCommand;
import com.fiap.techchallenge.inventory.api.commands.ReservePartCommand;
import com.fiap.techchallenge.inventory.api.events.PartStockLowEvent;
import com.fiap.techchallenge.inventory.api.queries.PurchaseOrderFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers the min/max order-up-to reorder policy end to end: the trigger uses inventory position
 * (available + inbound), not available alone, which is what stops a busy afternoon from placing the
 * same order twice — see ReorderRuleEvaluator's javadoc for the arithmetic this exercises.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@RecordApplicationEvents
class ReorderRuleFlowTest {

    @Autowired
    PartCatalogService partCatalogService;

    @Autowired
    VendorService vendorService;

    @Autowired
    ReorderRuleService reorderRuleService;

    @Autowired
    PartReservationService reservationService;

    @Autowired
    StockService stockService;

    @Autowired
    PurchaseOrderService purchaseOrderService;

    @Autowired
    ApplicationEvents events;

    @Test
    void reservingBelowMinAutoPlacesAPurchaseOrderUpToMax() {
        VendorInfo vendor = createVendor();
        PartInfo part = createPart("REORDER-1");
        reorderRuleService.create(new CreateReorderRuleCommand(part.id(), BigDecimal.valueOf(5), BigDecimal.valueOf(20), vendor.id(), true));

        reservationService.reserveForWorkOrder(UUID.randomUUID(), List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(6))));

        List<PurchaseOrderInfo> orders = purchaseOrdersFor(vendor.id());
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).lines().get(0).quantityOrdered()).isEqualByComparingTo("20");
    }

    @Test
    void secondReserveWithinCoveredPositionDoesNotDoubleOrder() {
        VendorInfo vendor = createVendor();
        PartInfo part = createPart("REORDER-2");
        reorderRuleService.create(new CreateReorderRuleCommand(part.id(), BigDecimal.valueOf(5), BigDecimal.valueOf(20), vendor.id(), true));

        // First reservation: position 0 <= 5, orders 20. inbound becomes 20, position becomes 20.
        reservationService.reserveForWorkOrder(UUID.randomUUID(), List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(6))));
        assertThat(purchaseOrdersFor(vendor.id())).hasSize(1);

        // Second reservation: available is still 0 (nothing received yet), but position is
        // available(0) + inbound(20) = 20, well above min — must not order again.
        reservationService.reserveForWorkOrder(UUID.randomUUID(), List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(3))));
        assertThat(purchaseOrdersFor(vendor.id())).hasSize(1);
    }

    @Test
    void disabledRulePublishesLowStockEventInsteadOfOrdering() {
        VendorInfo vendor = createVendor();
        PartInfo part = createPart("REORDER-3");
        stockService.adjust(part.id(), new AdjustStockCommand(BigDecimal.valueOf(10), "Seed above min"));
        reorderRuleService.create(new CreateReorderRuleCommand(part.id(), BigDecimal.valueOf(5), BigDecimal.valueOf(20), vendor.id(), false));

        reservationService.reserveForWorkOrder(UUID.randomUUID(), List.of(new ReservePartCommand(part.id(), BigDecimal.valueOf(6))));

        assertThat(purchaseOrdersFor(vendor.id())).isEmpty();

        List<PartStockLowEvent> lowStockEvents = events.stream(PartStockLowEvent.class)
                .filter(e -> e.partId().equals(part.id()))
                .toList();
        assertThat(lowStockEvents).hasSize(1);
        assertThat(lowStockEvents.get(0).minQuantity()).isEqualByComparingTo("5");
    }

    @Test
    void maxMustExceedMinAtCreation() {
        VendorInfo vendor = createVendor();
        PartInfo part = createPart("REORDER-4");

        assertThatThrownBy(() -> reorderRuleService.create(new CreateReorderRuleCommand(
                part.id(), BigDecimal.TEN, BigDecimal.TEN, vendor.id(), true)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void creatingARuleForAnAlreadyLowPartOrdersImmediately() {
        VendorInfo vendor = createVendor();
        PartInfo part = createPart("REORDER-5");

        stockService.adjust(part.id(), new AdjustStockCommand(BigDecimal.valueOf(3), "Seed below min"));

        reorderRuleService.create(new CreateReorderRuleCommand(part.id(), BigDecimal.valueOf(5), BigDecimal.valueOf(20), vendor.id(), true));

        // position = available(3) + inbound(0) = 3 <= 5 -> orders max - position = 17
        List<PurchaseOrderInfo> orders = purchaseOrdersFor(vendor.id());
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).lines().get(0).quantityOrdered()).isEqualByComparingTo("17");
    }

    // --- fixtures -----------------------------------------------------------------------------

    private List<PurchaseOrderInfo> purchaseOrdersFor(UUID vendorId) {
        Page<PurchaseOrderInfo> page = purchaseOrderService.listPurchaseOrders(
                new PurchaseOrderFilterQuery(vendorId, null, null), Pageable.unpaged());

        return page.getContent();
    }

    private VendorInfo createVendor() {
        return vendorService.create(new CreateVendorCommand("Reorder Vendor " + UUID.randomUUID(), null));
    }

    private PartInfo createPart(String sku) {
        return partCatalogService.create(new CreatePartCommand(
                sku, "Test Part " + sku, null, null, UnitOfMeasure.UNIT, BigDecimal.valueOf(50)));
    }
}

package com.fiap.techchallenge.inventory.schedules;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PurchaseOrderService;
import com.fiap.techchallenge.inventory.api.VendorService;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.CreateVendorCommand;
import com.fiap.techchallenge.inventory.api.queries.PurchaseOrderFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Safety net for the inline reorder checks (ReorderRuleFlowTest covers those): {@code create()}/
 * {@code update()} always evaluate their own rule immediately, so the only way to prove the sweep
 * itself does anything is to plant a rule that bypasses that inline path entirely — the exact "a rule
 * created against stale data" scenario the class's javadoc describes — and let the sweep be the only
 * thing that ever evaluates it.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ReorderSweepTest {

    @Autowired
    ReorderSweep job;

    @Autowired
    PartCatalogService partCatalogService;

    @Autowired
    VendorService vendorService;

    @Autowired
    PurchaseOrderService purchaseOrderService;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void sweepingEvaluatesARuleThatWasNeverEvaluatedInlineAndOrdersWhateverIsBelowMin() {
        VendorInfo vendor = vendorService.create(new CreateVendorCommand("Sweep Vendor", null));
        PartInfo part = partCatalogService.create(new CreatePartCommand(
                "SWEEP-1", "Test Part SWEEP-1", null, null, UnitOfMeasure.UNIT, BigDecimal.valueOf(50)));

        // Bypasses ReorderRuleServiceImpl.create(), which would otherwise evaluate the rule inline
        // and leave nothing for the sweep to catch.
        jdbcTemplate.update("""
                INSERT INTO inventory.reorder_rules (part_id, min_quantity, max_quantity, vendor_id, enabled)
                VALUES (?, 5, 20, ?, true)
                """, part.id(), vendor.id());

        // job.runSweep() evaluates every ReorderRule row in the database, not just this one — its
        // return value isn't asserted here since other tests in the full suite leave their own rules
        // behind; what matters is that the sweep evaluated *this* rule specifically.
        job.runSweep();

        Page<PurchaseOrderInfo> orders = purchaseOrderService.listPurchaseOrders(
                new PurchaseOrderFilterQuery(vendor.id(), null, null), Pageable.unpaged());
        assertThat(orders.getContent()).hasSize(1);
        assertThat(orders.getContent().get(0).lines().get(0).quantityOrdered()).isEqualByComparingTo("20");
    }
}

package com.fiap.techchallenge.inventory.services;

import java.util.UUID;

/**
 * Internal signal that a part's inventory position (available + inbound) may have dropped, published
 * wherever that can happen: a reservation claims stock, a manual adjustment removes it, a receipt's
 * shortfall top-up reallocates it, or a purchase order is cancelled. {@link ReorderRuleEvaluator}
 * listens and re-checks that part's rule.
 *
 * <p>Deliberately a plain Spring application event, not a {@code @NamedInterface}-exposed one in
 * {@code api.events}: it exists to let {@code PurchaseOrderServiceImpl} trigger re-evaluation without
 * holding a constructor reference to {@link ReorderRuleEvaluator}, which would cycle back through
 * {@code PurchaseOrderService} (see ReorderRuleEvaluator#evaluate). Nothing outside this package needs
 * to know it exists.
 */
record PartPositionMayHaveDroppedEvent(UUID partId) {
}

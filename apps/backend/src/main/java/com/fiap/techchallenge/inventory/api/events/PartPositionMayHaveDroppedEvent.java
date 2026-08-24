package com.fiap.techchallenge.inventory.api.events;

import com.fiap.techchallenge.inventory.services.StockPolicyEvaluator;

import java.util.UUID;

/**
 * Internal signal that a part's inventory position (available + inbound) may have dropped, published
 * wherever that can happen: a reservation claims stock, a manual adjustment removes it, a receipt's
 * shortfall top-up reallocates it, or a purchase order is cancelled. {@link StockPolicyEvaluator}
 * listens and re-checks that part's rule.
 *
 * <p>Deliberately a plain Spring application event, not a {@code @NamedInterface}-exposed one in
 * {@code api.events}: it exists to let {@code PurchaseOrderServiceImpl} trigger re-evaluation without
 * holding a constructor reference to {@link StockPolicyEvaluator}, which would cycle back through
 * {@code PurchaseOrderService} (see StockPolicyEvaluator#evaluate). Nothing outside this package needs
 * to know it exists.
 */
public record PartPositionMayHaveDroppedEvent(UUID partId) {
}

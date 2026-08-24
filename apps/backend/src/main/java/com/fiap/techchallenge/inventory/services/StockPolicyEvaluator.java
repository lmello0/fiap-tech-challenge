package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.api.PurchaseOrderService;
import com.fiap.techchallenge.inventory.api.commands.PlacePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.commands.PlacePurchaseOrderLineCommand;
import com.fiap.techchallenge.inventory.api.events.PartPositionMayHaveDroppedEvent;
import com.fiap.techchallenge.inventory.api.events.PartStockLowEvent;
import com.fiap.techchallenge.inventory.entities.StockPolicy;
import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import com.fiap.techchallenge.inventory.repositories.PurchaseOrderLineRepository;
import com.fiap.techchallenge.inventory.repositories.StockPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Evaluates one part's stock policy (min threshold, optional max order-up-to) and auto-places a
 * purchase order when it fires and auto-reorder is on. Called wherever a part's inventory
 * <b>position</b> — {@code available + inbound} — can drop: after a reservation claims stock, after a
 * manual adjustment, after a receipt (which moves stock from inbound to available but, combined with
 * the shortfall top-up that follows it, can still lower position), and after a purchase order is
 * cancelled (inbound disappears with nothing to replace it).
 *
 * <p>Deliberately <b>not</b> called after consuming a reservation ({@code startService}): consuming
 * moves the same quantity out of both {@code onHand} and {@code reserved} at once, so {@code
 * available} — and therefore position — is unchanged. That quantity's effect on position was already
 * evaluated when it was first reserved.
 *
 * <p>Callers trigger this via {@link PartPositionMayHaveDroppedEvent} rather than a direct method
 * call from every call site — {@code PurchaseOrderServiceImpl} needs to trigger it too, and a direct
 * constructor dependency from there back to this class would cycle, since evaluating can itself place
 * a purchase order through {@link PurchaseOrderService}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockPolicyEvaluator {

    private static final List<PurchaseOrderStatus> OPEN_STATUSES =
            List.of(PurchaseOrderStatus.PLACED, PurchaseOrderStatus.PARTIALLY_RECEIVED);

    private final StockPolicyRepository stockPolicyRepository;
    private final PurchaseOrderLineRepository purchaseOrderLineRepository;
    private final StockLedger ledger;
    private final PurchaseOrderService purchaseOrderService;
    private final ApplicationEventPublisher events;

    @EventListener
    void onPositionMayHaveDropped(PartPositionMayHaveDroppedEvent event) {
        evaluate(event.partId());
    }

    @Transactional
    public void evaluate(UUID partId) {
        Optional<StockPolicy> maybePolicy = stockPolicyRepository.findByPart_Id(partId);

        if (maybePolicy.isEmpty()) {
            return;
        }

        StockPolicy policy = maybePolicy.get();
        BigDecimal position = position(partId);

        if (position.compareTo(policy.getMinQuantity()) > 0) {
            return;
        }

        if (!policy.isAutoReorderEnabled()) {
            events.publishEvent(new PartStockLowEvent(partId, position, policy.getMinQuantity()));
            return;
        }

        BigDecimal quantity = policy.quantityToOrder(position);

        if (quantity.signum() <= 0) {
            log.warn("Stock policy for part {} fired at position {} but computed a non-positive quantity {}",
                    partId, position, quantity);
            return;
        }

        purchaseOrderService.place(new PlacePurchaseOrderCommand(
                policy.getVendor().getId(),
                List.of(new PlacePurchaseOrderLineCommand(partId, quantity))
        ));

        log.info("Auto-placed purchase order for part {}: position {} <= min {}, ordered {} up to max {}",
                partId, position, policy.getMinQuantity(), quantity, policy.getMaxQuantity());
    }

    private BigDecimal position(UUID partId) {
        BigDecimal inbound = purchaseOrderLineRepository.sumInboundForPart(partId, OPEN_STATUSES);

        return ledger.available(partId).add(inbound);
    }
}

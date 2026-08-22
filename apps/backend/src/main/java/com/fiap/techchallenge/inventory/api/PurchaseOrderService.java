package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.PlacePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.commands.ReceivePurchaseOrderCommand;
import com.fiap.techchallenge.inventory.api.queries.PurchaseOrderFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PurchaseOrderService {

    Page<PurchaseOrderInfo> listPurchaseOrders(PurchaseOrderFilterQuery filter, Pageable pageable);

    PurchaseOrderInfo getById(UUID id);

    /** Confirms the order with the vendor (mocked) and records it as PLACED. */
    PurchaseOrderInfo place(PlacePurchaseOrderCommand command);

    /**
     * Records what actually arrived, per line, at the cost it arrived at. Advances the purchase
     * order to PARTIALLY_RECEIVED or RECEIVED depending on whether every line is now complete, rolls
     * the received cost into each part's moving-average cost, and tops up that part's oldest
     * outstanding reservation shortfalls with whatever the receipt freed up.
     */
    PurchaseOrderInfo receive(UUID purchaseOrderId, ReceivePurchaseOrderCommand command);

    /** Only legal while PLACED or PARTIALLY_RECEIVED — a RECEIVED or already-CANCELLED order can't change further. */
    PurchaseOrderInfo cancel(UUID id);
}

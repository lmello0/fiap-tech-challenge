package com.fiap.techchallenge.inventory.services;

import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import com.fiap.techchallenge.inventory.exceptions.IllegalPurchaseOrderStateException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class PurchaseOrderStateMachine {

    /**
     * PARTIALLY_RECEIVED transitions to itself: a receipt that doesn't complete every line leaves
     * the purchase order exactly where it was, but it's still a legal outcome of "receive" that the
     * caller resolves through this same transition() call rather than special-casing "no change".
     */
    private static final Map<PurchaseOrderStatus, Set<PurchaseOrderStatus>> TRANSITIONS = Map.of(
            PurchaseOrderStatus.PLACED, EnumSet.of(
                    PurchaseOrderStatus.PARTIALLY_RECEIVED,
                    PurchaseOrderStatus.RECEIVED,
                    PurchaseOrderStatus.CANCELLED),
            PurchaseOrderStatus.PARTIALLY_RECEIVED, EnumSet.of(
                    PurchaseOrderStatus.PARTIALLY_RECEIVED,
                    PurchaseOrderStatus.RECEIVED,
                    PurchaseOrderStatus.CANCELLED),
            PurchaseOrderStatus.RECEIVED, EnumSet.noneOf(PurchaseOrderStatus.class),
            PurchaseOrderStatus.CANCELLED, EnumSet.noneOf(PurchaseOrderStatus.class)
    );

    public boolean canTransition(PurchaseOrderStatus from, PurchaseOrderStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(PurchaseOrderStatus.class)).contains(to);
    }

    public PurchaseOrderStatus transition(PurchaseOrderStatus from, PurchaseOrderStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalPurchaseOrderStateException(from, to);
        }

        return to;
    }

    public Set<PurchaseOrderStatus> allowedNext(PurchaseOrderStatus from) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(PurchaseOrderStatus.class));
    }

    public boolean isTerminal(PurchaseOrderStatus status) {
        return allowedNext(status).isEmpty();
    }
}

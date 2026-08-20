package com.fiap.techchallenge.inventory.exceptions;

import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import lombok.Getter;

@Getter
public class IllegalPurchaseOrderStateException extends RuntimeException {

    private final PurchaseOrderStatus from;
    private final PurchaseOrderStatus to;

    public IllegalPurchaseOrderStateException(PurchaseOrderStatus from, PurchaseOrderStatus to) {
        super("Illegal purchase order transition: " + from + " -> " + to);

        this.from = from;
        this.to = to;
    }
}

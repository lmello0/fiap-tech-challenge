package com.fiap.techchallenge.inventory.enums;

/**
 * The reason a {@link com.fiap.techchallenge.inventory.entities.StockMovement} changed
 * {@code quantityOnHand}. Reservations never appear here — they claim stock without moving it.
 */
public enum StockMovementType {
    /** Stock arriving from a vendor receipt. Always a positive quantity. */
    PURCHASE,
    /** Stock leaving through a work order. Always a negative quantity. */
    CONSUMPTION,
    /** A manual correction against a physical count, breakage, or loss. Signed either way. */
    ADJUSTMENT
}

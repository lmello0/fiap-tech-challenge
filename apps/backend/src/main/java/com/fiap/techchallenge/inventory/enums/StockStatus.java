package com.fiap.techchallenge.inventory.enums;

/**
 * A part's derived stock standing, shown on the catalog list so a worker can filter for parts that
 * need attention without reading raw quantities.
 */
public enum StockStatus {
    /** Available quantity is zero or below. */
    OUT,
    /** A {@code StockPolicy} exists and available quantity is at or below its {@code minQuantity}. */
    LOW,
    /** A {@code StockPolicy} exists and available quantity is above its {@code minQuantity}. */
    OK,
    /** No {@code StockPolicy} has ever been set for this part — nobody has decided what "low" means for it. */
    NO_POLICY
}

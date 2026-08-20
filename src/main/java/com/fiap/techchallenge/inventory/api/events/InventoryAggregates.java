package com.fiap.techchallenge.inventory.api.events;

/** The distinct Timelines (CONTEXT.md) inventory events belong to. A Reorder Rule is per-part
 * standing instruction, so its events belong to the owning Part's Timeline, not their own. */
public final class InventoryAggregates {

    public static final String PART = "PART";
    public static final String PURCHASE_ORDER = "PURCHASE_ORDER";
    public static final String REPAIR_SERVICE = "REPAIR_SERVICE";
    public static final String VENDOR = "VENDOR";

    private InventoryAggregates() {
    }
}

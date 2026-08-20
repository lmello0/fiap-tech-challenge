package com.fiap.techchallenge.inventory.vendor;

/**
 * The one door {@code inventory} has onto a vendor's ordering system. There is no real vendor API
 * for this project — {@link MockPartVendorClient} is the only implementation — but the boundary is
 * shaped so a real HTTP client could drop in without PurchaseOrderService changing at all.
 */
public interface PartVendorClient {

    VendorOrderConfirmation placeOrder(VendorOrderRequest request);
}

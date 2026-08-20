package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.PurchaseOrder;
import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class PurchaseOrderSpecifications {

    public static Specification<PurchaseOrder> vendorIdEquals(UUID vendorId) {
        return (root, query, cb) ->
                vendorId == null ? null : cb.equal(root.get("vendor").get("id"), vendorId);
    }

    public static Specification<PurchaseOrder> statusEquals(PurchaseOrderStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<PurchaseOrder> codeEquals(String code) {
        return (root, query, cb) ->
                code == null ? null : cb.equal(root.get("code"), code);
    }

    private PurchaseOrderSpecifications() {
    }
}

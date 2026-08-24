package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.StockPolicy;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class StockPolicySpecifications {

    public static Specification<StockPolicy> partIdEquals(UUID partId) {
        return (root, query, cb) ->
                partId == null ? null : cb.equal(root.get("part").get("id"), partId);
    }

    public static Specification<StockPolicy> vendorIdEquals(UUID vendorId) {
        return (root, query, cb) ->
                vendorId == null ? null : cb.equal(root.get("vendor").get("id"), vendorId);
    }

    public static Specification<StockPolicy> autoReorderEnabledEquals(Boolean autoReorderEnabled) {
        return (root, query, cb) ->
                autoReorderEnabled == null ? null : cb.equal(root.get("autoReorderEnabled"), autoReorderEnabled);
    }

    private StockPolicySpecifications() {
    }
}

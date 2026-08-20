package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.ReorderRule;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

public class ReorderRuleSpecifications {

    public static Specification<ReorderRule> partIdEquals(UUID partId) {
        return (root, query, cb) ->
                partId == null ? null : cb.equal(root.get("part").get("id"), partId);
    }

    public static Specification<ReorderRule> vendorIdEquals(UUID vendorId) {
        return (root, query, cb) ->
                vendorId == null ? null : cb.equal(root.get("vendor").get("id"), vendorId);
    }

    public static Specification<ReorderRule> enabledEquals(Boolean enabled) {
        return (root, query, cb) ->
                enabled == null ? null : cb.equal(root.get("enabled"), enabled);
    }

    private ReorderRuleSpecifications() {
    }
}

package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.RepairService;
import org.springframework.data.jpa.domain.Specification;

public class RepairServiceSpecifications {

    public static Specification<RepairService> codeEquals(String code) {
        return (root, query, cb) ->
                code == null ? null : cb.equal(root.get("code"), code);
    }

    public static Specification<RepairService> nameContains(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<RepairService> activeEquals(Boolean active) {
        return (root, query, cb) ->
                active == null ? null : cb.equal(root.get("active"), active);
    }

    private RepairServiceSpecifications() {
    }
}

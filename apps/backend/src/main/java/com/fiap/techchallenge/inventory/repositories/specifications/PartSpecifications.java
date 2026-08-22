package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class PartSpecifications {

    public static Specification<Part> skuEquals(String sku) {
        return (root, query, cb) ->
                sku == null ? null : cb.equal(root.get("sku"), sku);
    }

    public static Specification<Part> nameContains(String name) {
        return (root, query, cb) ->
                name == null ? null : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Part> brandEquals(String brand) {
        return (root, query, cb) ->
                brand == null ? null : cb.equal(root.get("brand"), brand);
    }

    public static Specification<Part> unitOfMeasureIn(List<UnitOfMeasure> unitOfMeasures) {
        return (root, query, cb) -> {
            if (unitOfMeasures == null || unitOfMeasures.isEmpty()) {
                return null;
            }

            return cb.in(root.get("unitOfMeasure").in(unitOfMeasures));
        };
    }

    public static Specification<Part> activeEquals(Boolean active) {
        return (root, query, cb) ->
                active == null ? null : cb.equal(root.get("active"), active);
    }

    private PartSpecifications() {
    }
}

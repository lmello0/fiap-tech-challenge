package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.PartStock;
import com.fiap.techchallenge.inventory.enums.StockStatus;
import org.springframework.data.jpa.domain.Specification;

public class PartStockSpecifications {

    public static Specification<PartStock> stockStatusEquals(StockStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("stockStatus"), status);
    }

    private PartStockSpecifications() {
    }
}

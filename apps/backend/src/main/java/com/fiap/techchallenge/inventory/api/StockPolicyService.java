package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.CreateStockPolicyCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateStockPolicyCommand;
import com.fiap.techchallenge.inventory.api.queries.StockPolicyFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.StockPolicyInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface StockPolicyService {

    Page<StockPolicyInfo> listStockPolicys(StockPolicyFilterQuery filter, Pageable pageable);

    StockPolicyInfo getById(UUID id);

    StockPolicyInfo create(CreateStockPolicyCommand command);

    StockPolicyInfo update(UUID id, UpdateStockPolicyCommand command);

    void delete(UUID id);
}

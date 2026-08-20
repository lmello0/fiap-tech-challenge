package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.CreateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateReorderRuleCommand;
import com.fiap.techchallenge.inventory.api.queries.ReorderRuleFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.ReorderRuleInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ReorderRuleService {

    Page<ReorderRuleInfo> listReorderRules(ReorderRuleFilterQuery filter, Pageable pageable);

    ReorderRuleInfo getById(UUID id);

    ReorderRuleInfo create(CreateReorderRuleCommand command);

    ReorderRuleInfo update(UUID id, UpdateReorderRuleCommand command);

    void delete(UUID id);
}

package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdatePartCommand;
import com.fiap.techchallenge.inventory.api.queries.PartFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PartCatalogService {

    Page<PartInfo> listParts(PartFilterQuery filter, Pageable pageable);

    PartInfo getById(UUID id);

    PartInfo create(CreatePartCommand command);

    PartInfo update(UUID id, UpdatePartCommand command);

    void deactivate(UUID id);
}

package com.fiap.techchallenge.inventory.api;

import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.commands.UpdateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.queries.RepairServiceFilterQuery;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface RepairServiceCatalogService {

    Page<RepairServiceInfo> listServices(RepairServiceFilterQuery filter, Pageable pageable);

    RepairServiceInfo getById(UUID id);

    RepairServiceInfo create(CreateRepairServiceCommand command);

    RepairServiceInfo update(UUID id, UpdateRepairServiceCommand command);

    void deactivate(UUID id);

    /**
     * Appends one measured sample of how long the service took on a work order row, then recomputes
     * {@code averageSeconds} over the most recent {@code app.inventory.average-window} samples — a
     * rolling average, not an all-time one, so a shop that gets faster isn't anchored to its first
     * year. {@code workOrderRowId} may only be recorded once; a row can be started and finished
     * exactly once.
     */
    void recordExecution(UUID repairServiceId, UUID workOrderId, UUID workOrderRowId, int durationSeconds);
}

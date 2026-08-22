package com.fiap.techchallenge.vehicle.api;

import com.fiap.techchallenge.vehicle.api.commands.CreateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.commands.UpdateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.queries.VehicleFilterQuery;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface VehicleService {

    Page<VehicleInfo> listVehicles(VehicleFilterQuery filter, Pageable pageable);

    VehicleInfo getById(UUID id);

    VehicleInfo create(CreateVehicleCommand command);

    VehicleInfo update(UUID id, UpdateVehicleCommand command);

    void deactivate(UUID id);
}

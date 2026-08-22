package com.fiap.techchallenge.vehicle.mappers;

import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
import com.fiap.techchallenge.vehicle.entities.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VehicleMapper {

    VehicleInfo toInfo(Vehicle vehicle);
}

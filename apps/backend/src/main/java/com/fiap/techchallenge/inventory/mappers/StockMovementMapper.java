package com.fiap.techchallenge.inventory.mappers;

import com.fiap.techchallenge.inventory.api.representation.StockMovementInfo;
import com.fiap.techchallenge.inventory.entities.StockMovement;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockMovementMapper {

    @Mapping(target = "partId", source = "part.id")
    StockMovementInfo toInfo(StockMovement movement);
}

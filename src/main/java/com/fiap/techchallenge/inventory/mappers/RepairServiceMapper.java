package com.fiap.techchallenge.inventory.mappers;

import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import com.fiap.techchallenge.inventory.entities.RepairService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface RepairServiceMapper {

    @Mapping(target = "averageSeconds", expression = "java(repairService.getEffectiveAverageSeconds())")
    RepairServiceInfo toInfo(RepairService repairService);
}

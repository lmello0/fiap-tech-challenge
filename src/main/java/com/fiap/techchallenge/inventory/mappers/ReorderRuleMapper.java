package com.fiap.techchallenge.inventory.mappers;

import com.fiap.techchallenge.inventory.api.representation.ReorderRuleInfo;
import com.fiap.techchallenge.inventory.entities.ReorderRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ReorderRuleMapper {

    @Mapping(target = "partId", source = "part.id")
    @Mapping(target = "vendorId", source = "vendor.id")
    ReorderRuleInfo toInfo(ReorderRule rule);
}

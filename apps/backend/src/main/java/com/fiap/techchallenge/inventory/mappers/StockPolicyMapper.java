package com.fiap.techchallenge.inventory.mappers;

import com.fiap.techchallenge.inventory.api.representation.StockPolicyInfo;
import com.fiap.techchallenge.inventory.entities.StockPolicy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StockPolicyMapper {

    @Mapping(target = "partId", source = "part.id")
    @Mapping(target = "vendorId", source = "vendor.id")
    StockPolicyInfo toInfo(StockPolicy policy);
}

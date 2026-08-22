package com.fiap.techchallenge.inventory.mappers;

import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderLineInfo;
import com.fiap.techchallenge.inventory.entities.PurchaseOrderLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface PurchaseOrderLineMapper {

    @Mapping(target = "partId", source = "part.id")
    PurchaseOrderLineInfo toInfo(PurchaseOrderLine line);
}

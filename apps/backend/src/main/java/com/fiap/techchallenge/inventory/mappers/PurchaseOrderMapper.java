package com.fiap.techchallenge.inventory.mappers;

import com.fiap.techchallenge.inventory.api.representation.PurchaseOrderInfo;
import com.fiap.techchallenge.inventory.entities.PurchaseOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = PurchaseOrderLineMapper.class)
public interface PurchaseOrderMapper {

    @Mapping(target = "vendorId", source = "vendor.id")
    PurchaseOrderInfo toInfo(PurchaseOrder purchaseOrder);
}

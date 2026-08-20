package com.fiap.techchallenge.inventory.mappers;

import com.fiap.techchallenge.inventory.api.representation.VendorInfo;
import com.fiap.techchallenge.inventory.entities.Vendor;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VendorMapper {

    VendorInfo toInfo(Vendor vendor);
}

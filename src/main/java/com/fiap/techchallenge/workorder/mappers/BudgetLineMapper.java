package com.fiap.techchallenge.workorder.mappers;

import com.fiap.techchallenge.workorder.api.representation.BudgetLineInfo;
import com.fiap.techchallenge.workorder.entities.BudgetLine;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface BudgetLineMapper {

    @Mapping(target = "lineTotal", source = "total")
    BudgetLineInfo toInfo(BudgetLine line);
}

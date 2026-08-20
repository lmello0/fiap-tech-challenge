package com.fiap.techchallenge.workorder.mappers;

import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
import com.fiap.techchallenge.workorder.entities.Budget;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = BudgetLineMapper.class)
public interface BudgetMapper {

    @Mapping(target = "grandTotal", source = "grandTotal")
    BudgetInfo toInfo(Budget budget);
}

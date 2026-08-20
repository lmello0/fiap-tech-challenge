package com.fiap.techchallenge.workorder.mappers;

import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import com.fiap.techchallenge.workorder.entities.Budget;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

import java.util.UUID;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkOrderMapper {

    @Mapping(target = "budgetId", expression = "java(currentBudgetId(workOrder))")
    WorkOrderInfo toInfo(WorkOrder workOrder);

    default UUID currentBudgetId(WorkOrder workOrder) {
        Budget budget = workOrder.getCurrentBudget();
        return budget != null ? budget.getId() : null;
    }
}

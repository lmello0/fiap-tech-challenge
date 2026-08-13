package com.fiap.techchallenge.workorder.mappers;

import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderRowCommand;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderRowInfo;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.entities.WorkOrderRow;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface WorkOrderRowMapper {

    WorkOrderRow fromCreateCommand(CreateWorkOrderRowCommand command);

    WorkOrderRowInfo toInfo(WorkOrder workOrder);
}

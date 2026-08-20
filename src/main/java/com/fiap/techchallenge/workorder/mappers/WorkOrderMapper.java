package com.fiap.techchallenge.workorder.mappers;

import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, uses = WorkOrderRowMapper.class)
public interface WorkOrderMapper {

    WorkOrderInfo toInfo(WorkOrder workOrder);

}

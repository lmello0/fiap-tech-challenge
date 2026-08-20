package com.fiap.techchallenge.scheduling.mappers;

import com.fiap.techchallenge.scheduling.api.representation.ClosureInfo;
import com.fiap.techchallenge.scheduling.entities.Closure;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface ClosureMapper {

    ClosureInfo toInfo(Closure closure);
}

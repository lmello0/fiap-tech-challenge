package com.fiap.techchallenge.scheduling.mappers;

import com.fiap.techchallenge.scheduling.api.representation.SchedulingSettingsInfo;
import com.fiap.techchallenge.scheduling.entities.SchedulingSettings;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SchedulingSettingsMapper {

    SchedulingSettingsInfo toInfo(SchedulingSettings settings);
}

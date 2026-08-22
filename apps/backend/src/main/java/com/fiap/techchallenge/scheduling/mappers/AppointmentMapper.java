package com.fiap.techchallenge.scheduling.mappers;

import com.fiap.techchallenge.scheduling.api.representation.AppointmentInfo;
import com.fiap.techchallenge.scheduling.entities.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface AppointmentMapper {

    @Mapping(target = "slotEnd", expression = "java(appointment.slotEnd())")
    AppointmentInfo toInfo(Appointment appointment);
}

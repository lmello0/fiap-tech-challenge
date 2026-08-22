package com.fiap.techchallenge.user.mappers;

import com.fiap.techchallenge.user.api.representation.PhoneNumberInfo;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.entities.PhoneNumber;
import com.fiap.techchallenge.user.entities.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    @Mapping(target = "customer", expression = "java(user.isCustomer())")
    @Mapping(target = "customerActive", expression = "java(user.isCustomer() && user.getCustomer().isActive())")
    @Mapping(target = "worker", expression = "java(user.isWorker())")
    @Mapping(target = "workerRole", source = "worker.role")
    @Mapping(target = "registration", source = "worker.registration")
    @Mapping(target = "hireDate", source = "worker.hireDate")
    @Mapping(target = "terminationDate", source = "worker.terminationDate")
    UserInfo toInfo(User user);

    PhoneNumberInfo toInfo(PhoneNumber phoneNumber);
}

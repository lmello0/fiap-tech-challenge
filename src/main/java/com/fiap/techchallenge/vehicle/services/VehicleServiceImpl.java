package com.fiap.techchallenge.vehicle.services;

import com.fiap.techchallenge.vehicle.api.VehicleService;
import com.fiap.techchallenge.vehicle.api.commands.CreateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.commands.UpdateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.queries.VehicleFilterQuery;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
import com.fiap.techchallenge.vehicle.entities.Vehicle;
import com.fiap.techchallenge.vehicle.exceptions.VehicleNotFoundException;
import com.fiap.techchallenge.vehicle.mappers.VehicleMapper;
import com.fiap.techchallenge.vehicle.repositories.VehicleRepository;
import com.fiap.techchallenge.vehicle.repositories.specifications.VehicleSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;

    private final VehicleMapper vehicleMapper;

    @Override
    @Transactional(readOnly = true)
    public Page<VehicleInfo> listVehicles(VehicleFilterQuery filter, Pageable pageable) {
        Specification<Vehicle> spec = Specification
                .where(VehicleSpecifications.hasCustomerId(filter.customerId()))
                .and(VehicleSpecifications.typeIn(filter.vehicleTypes()))
                .and(VehicleSpecifications.licensePlateEquals(filter.licensePlate()))
                .and(VehicleSpecifications.makeEquals(filter.make()))
                .and(VehicleSpecifications.modelEquals(filter.model()))
                .and(VehicleSpecifications.colorEquals(filter.color()))
                .and(VehicleSpecifications.modelYearBetween(filter.modelYearStart(), filter.modelYearEnd()))
                .and(VehicleSpecifications.manufactureYearBetween(filter.manufactureYearStart(), filter.manufactureYearEnd()))
                .and(VehicleSpecifications.fuelTypeIn(filter.fuelTypes()))
                .and(VehicleSpecifications.transmissionTypeIn(filter.transmissionTypes()))
                .and(VehicleSpecifications.createdBetween(filter.createdAtStart(), filter.createdAtEnd()));

        return vehicleRepository
                .findAll(spec, pageable)
                .map(vehicleMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleInfo getById(UUID id) {
        return vehicleRepository
                .findById(id)
                .map(vehicleMapper::toInfo)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }

    @Override
    @Transactional
    public VehicleInfo create(CreateVehicleCommand command) {
        Vehicle vehicle = new Vehicle();

        vehicle.setCustomerId(command.customerId());
        vehicle.setVehicleType(command.vehicleType());
        vehicle.setLicensePlate(command.licensePlate());
        vehicle.setMake(command.make());
        vehicle.setModel(command.model());
        vehicle.setColor(command.color());
        vehicle.setModelYear(command.modelYear());
        vehicle.setManufactureYear(command.manufactureYear());
        vehicle.setVersion(command.version());
        vehicle.setFuelType(command.fuelType());
        vehicle.setTransmissionType(command.transmissionType());

        vehicleRepository.save(vehicle);

        return vehicleMapper.toInfo(vehicle);
    }

    @Override
    @Transactional
    public VehicleInfo update(UUID id, UpdateVehicleCommand command) {
        Vehicle vehicle = load(id);

        vehicle.setVehicleType(command.vehicleType());
        vehicle.setLicensePlate(command.licensePlate());
        vehicle.setMake(command.make());
        vehicle.setModel(command.model());
        vehicle.setColor(command.color());
        vehicle.setModelYear(command.modelYear());
        vehicle.setManufactureYear(command.manufactureYear());
        vehicle.setVersion(command.version());
        vehicle.setFuelType(command.fuelType());
        vehicle.setTransmissionType(command.transmissionType());

        return vehicleMapper.toInfo(vehicle);
    }

    @Override
    @Transactional
    public void deactivate(UUID id) {
        Vehicle vehicle = load(id);

        vehicle.deactivate();
    }

    private Vehicle load(UUID id) {
        return vehicleRepository
                .findById(id)
                .orElseThrow(() -> new VehicleNotFoundException(id));
    }
}

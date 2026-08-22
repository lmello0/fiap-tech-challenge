package com.fiap.techchallenge.vehicle.repositories.specifications;

import com.fiap.techchallenge.vehicle.entities.Vehicle;
import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class VehicleSpecifications {

    public static Specification<Vehicle> hasCustomerId(UUID customerId) {
        return (root, query, cb) ->
                customerId == null ? null : cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<Vehicle> typeIn(List<VehicleType> vehicleTypes) {
        return (root, query, cb) -> {
            if (vehicleTypes == null || vehicleTypes.isEmpty()) {
                return null;
            }

            return cb.in(root.get("vehicleType").in(vehicleTypes));
        };
    }

    public static Specification<Vehicle> licensePlateEquals(String licensePlate) {
        return (root, query, cb) ->
                licensePlate == null ? null : cb.equal(root.get("licensePlate"), licensePlate);
    }

    public static Specification<Vehicle> makeEquals(String make) {
        return (root, query, cb) ->
                make == null ? null : cb.equal(root.get("make"), make);
    }

    public static Specification<Vehicle> modelEquals(String model) {
        return (root, query, cb) ->
                model == null ? null : cb.equal(root.get("model"), model);
    }

    public static Specification<Vehicle> colorEquals(String color) {
        return (root, query, cb) ->
                color == null ? null : cb.equal(root.get("color"), color);
    }

    public static Specification<Vehicle> modelYearBetween(Integer start, Integer end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return null;
            }

            if (start == null) {
                return cb.lessThanOrEqualTo(root.get("modelYear"), end);
            }

            if (end == null) {
                return cb.greaterThanOrEqualTo(root.get("modelYear"), start);
            }

            return cb.between(root.get("modelYear"), start, end);
        };
    }

    public static Specification<Vehicle> manufactureYearBetween(Integer start, Integer end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return null;
            }

            if (start == null) {
                return cb.lessThanOrEqualTo(root.get("manufactureYear"), end);
            }

            if (end == null) {
                return cb.greaterThanOrEqualTo(root.get("manufactureYear"), start);
            }

            return cb.between(root.get("manufactureYear"), start, end);
        };
    }

    public static Specification<Vehicle> fuelTypeIn(List<FuelType> fuelTypes) {
        return (root, query, cb) -> {
            if (fuelTypes == null || fuelTypes.isEmpty()) {
                return null;
            }

            return cb.in(root.get("fuelType").in(fuelTypes));
        };
    }

    public static Specification<Vehicle> transmissionTypeIn(List<TransmissionType> transmissionTypes) {
        return (root, query, cb) -> {
            if (transmissionTypes == null || transmissionTypes.isEmpty()) {
                return null;
            }

            return cb.in(root.get("transmissionType").in(transmissionTypes));
        };
    }

    public static Specification<Vehicle> createdBetween(LocalDate start, LocalDate end) {
        return (root, query, cb) -> {
            if (start == null && end == null) {
                return null;
            }

            if (start == null) {
                return cb.lessThanOrEqualTo(root.get("createdAt"), end);
            }

            if (end == null) {
                return cb.greaterThanOrEqualTo(root.get("createdAt"), start);
            }

            return cb.between(root.get("createdAt"), start, end);
        };
    }
}

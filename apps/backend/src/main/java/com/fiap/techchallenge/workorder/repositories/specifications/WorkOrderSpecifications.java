package com.fiap.techchallenge.workorder.repositories.specifications;

import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public class WorkOrderSpecifications {

    public static Specification<WorkOrder> belongsToCustomerId(UUID customerId) {
        return (root, query, cb) ->
                customerId == null ? null : cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<WorkOrder> belongsToCustomerName(String customerName) {
        return (root, query, cb) ->
                customerName == null ? null : cb.equal(root.get("customerName"), customerName);
    }

    public static Specification<WorkOrder> ofVehicleId(UUID vehicleId) {
        return (root, query, cb) ->
                vehicleId == null ? null : cb.equal(root.get("vehicleId"), vehicleId);
    }

    public static Specification<WorkOrder> ofVehiclePlate(String vehiclePlate) {
        return (root, query, cb) ->
                vehiclePlate == null ? null : cb.equal(root.get("vehiclePlate"), vehiclePlate);
    }

    public static Specification<WorkOrder> ofVehicleMake(String vehicleMake) {
        return (root, query, cb) ->
                vehicleMake == null ? null : cb.equal(root.get("vehicleMake"), vehicleMake);
    }

    public static Specification<WorkOrder> ofVehicleModel(String vehicleModel) {
        return (root, query, cb) ->
                vehicleModel == null ? null : cb.equal(root.get("vehicleModel"), vehicleModel);
    }

    public static Specification<WorkOrder> withMechanicId(UUID mechanicId) {
        return (root, query, cb) ->
                mechanicId == null ? null : cb.equal(root.get("mechanicId"), mechanicId);
    }

    public static Specification<WorkOrder> withMechanicName(String mechanicName) {
        return (root, query, cb) ->
                mechanicName == null ? null : cb.equal(root.get("mechanicName"), mechanicName);
    }

    public static Specification<WorkOrder> withStatus(Set<WorkOrderStatus> statuses) {
        return (root, query, cb) ->
                statuses == null || statuses.isEmpty() ? null : cb.in(root.get("status").in(statuses));
    }

    public static Specification<WorkOrder> withCode(String code) {
        return (root, query, cb) ->
                code == null ? null : cb.equal(root.get("code"), code);
    }

    public static Specification<WorkOrder> createdBetween(LocalDate start, LocalDate end) {
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

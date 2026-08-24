package com.fiap.techchallenge.workorder.repositories.specifications;

import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class WorkOrderSpecifications {

    public static Specification<WorkOrder> belongsToCustomerId(UUID customerId) {
        return (root, query, cb) ->
                customerId == null ? null : cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<WorkOrder> belongsToCustomerNameLike(String customerName) {
        return (root, query, cb) ->
                customerName == null ? null : cb.like(cb.lower(root.get("customerName")), "%" + customerName.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<WorkOrder> ofVehicleId(UUID vehicleId) {
        return (root, query, cb) ->
                vehicleId == null ? null : cb.equal(root.get("vehicleId"), vehicleId);
    }

    public static Specification<WorkOrder> ofVehiclePlateLike(String vehiclePlate) {
        return (root, query, cb) ->
                vehiclePlate == null ? null : cb.like(cb.lower(root.get("vehiclePlate")), "%" + vehiclePlate.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<WorkOrder> ofVehicleMakeLike(String vehicleMake) {
        return (root, query, cb) ->
                vehicleMake == null ? null : cb.like(cb.lower(root.get("vehicleMake")), "%" + vehicleMake.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<WorkOrder> ofVehicleModelLike(String vehicleModel) {
        return (root, query, cb) ->
                vehicleModel == null ? null : cb.like(cb.lower(root.get("vehicleModel")), "%" + vehicleModel.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<WorkOrder> withMechanicId(UUID mechanicId) {
        return (root, query, cb) ->
                mechanicId == null ? null : cb.equal(root.get("mechanicId"), mechanicId);
    }

    public static Specification<WorkOrder> withMechanicNameLike(String mechanicName) {
        return (root, query, cb) ->
                mechanicName == null ? null : cb.like(cb.lower(root.get("mechanicName")), "%" + mechanicName.toLowerCase(Locale.ROOT) + "%");
    }

    public static Specification<WorkOrder> withStatus(Set<WorkOrderStatus> statuses) {
        return (root, query, cb) ->
                statuses == null || statuses.isEmpty() ? null : root.get("status").in(statuses);
    }

    public static Specification<WorkOrder> withCode(String code) {
        return (root, query, cb) ->
                code == null ? null : cb.like(cb.lower(root.get("orderCode")), "%" + code.toLowerCase(Locale.ROOT) + "%");
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

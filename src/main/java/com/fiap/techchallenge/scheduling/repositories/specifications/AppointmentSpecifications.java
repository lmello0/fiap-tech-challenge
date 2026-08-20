package com.fiap.techchallenge.scheduling.repositories.specifications;

import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public class AppointmentSpecifications {

    private static final ZoneId SHOP_ZONE = ZoneId.systemDefault();

    public static Specification<Appointment> ofType(AppointmentType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("type"), type);
    }

    public static Specification<Appointment> withStatus(AppointmentStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Appointment> belongsToCustomer(UUID customerId) {
        return (root, query, cb) -> customerId == null ? null : cb.equal(root.get("customerId"), customerId);
    }

    public static Specification<Appointment> ofWorkOrder(UUID workOrderId) {
        return (root, query, cb) -> workOrderId == null ? null : cb.equal(root.get("workOrderId"), workOrderId);
    }

    public static Specification<Appointment> onDate(LocalDate date) {
        return (root, query, cb) -> {
            if (date == null) {
                return null;
            }

            Instant start = date.atStartOfDay(SHOP_ZONE).toInstant();
            Instant end = date.plusDays(1).atStartOfDay(SHOP_ZONE).toInstant();

            return cb.between(root.get("slotStart"), start, end);
        };
    }
}

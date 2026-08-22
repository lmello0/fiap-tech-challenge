package com.fiap.techchallenge.scheduling.repositories.specifications;

import com.fiap.techchallenge.scheduling.entities.Appointment;
import com.fiap.techchallenge.scheduling.enums.AppointmentStatus;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Each factory method is just a null-check branch: filter present -> build a predicate, filter
 * absent -> null (Specification's no-op). Mocked Criteria API is enough to prove both branches
 * without touching a real database.
 */
class AppointmentSpecificationsTest {

    private final Root<Appointment> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void ofTypeIsANoOpWhenTypeIsNull() {
        Specification<Appointment> spec = AppointmentSpecifications.ofType(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void ofTypeFiltersByTypeWhenPresent() {
        Path<Object> typePath = mockPath("type");
        when(cb.equal(typePath, AppointmentType.PICKUP)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecifications.ofType(AppointmentType.PICKUP);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(typePath, AppointmentType.PICKUP);
    }

    @Test
    void withStatusIsANoOpWhenStatusIsNull() {
        Specification<Appointment> spec = AppointmentSpecifications.withStatus(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void withStatusFiltersByStatusWhenPresent() {
        Path<Object> statusPath = mockPath("status");
        when(cb.equal(statusPath, AppointmentStatus.NO_SHOW)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecifications.withStatus(AppointmentStatus.NO_SHOW);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(statusPath, AppointmentStatus.NO_SHOW);
    }

    @Test
    void belongsToCustomerIsANoOpWhenCustomerIdIsNull() {
        Specification<Appointment> spec = AppointmentSpecifications.belongsToCustomer(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void belongsToCustomerFiltersByCustomerIdWhenPresent() {
        UUID customerId = UUID.randomUUID();
        Path<Object> customerPath = mockPath("customerId");
        when(cb.equal(customerPath, customerId)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecifications.belongsToCustomer(customerId);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(customerPath, customerId);
    }

    @Test
    void ofWorkOrderIsANoOpWhenWorkOrderIdIsNull() {
        Specification<Appointment> spec = AppointmentSpecifications.ofWorkOrder(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void ofWorkOrderFiltersByWorkOrderIdWhenPresent() {
        UUID workOrderId = UUID.randomUUID();
        Path<Object> workOrderPath = mockPath("workOrderId");
        when(cb.equal(workOrderPath, workOrderId)).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecifications.ofWorkOrder(workOrderId);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(workOrderPath, workOrderId);
    }

    @Test
    void onDateIsANoOpWhenDateIsNull() {
        Specification<Appointment> spec = AppointmentSpecifications.onDate(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void onDateFiltersBetweenStartAndEndOfDayWhenPresent() {
        LocalDate date = LocalDate.of(2026, 3, 10);
        Path<Instant> slotStartPath = mock();
        when(root.<Instant>get("slotStart")).thenReturn(slotStartPath);
        when(cb.between(eq(slotStartPath), any(Instant.class), any(Instant.class))).thenReturn(predicate);

        Specification<Appointment> spec = AppointmentSpecifications.onDate(date);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).between(eq(slotStartPath), any(Instant.class), any(Instant.class));
    }

    private Path<Object> mockPath(String attribute) {
        Path<Object> path = mock();
        when(root.<Object>get(attribute)).thenReturn(path);
        return path;
    }

    private void verifyNoInteractionsWithCriteriaBuilder() {
        Mockito.verifyNoInteractions(cb);
    }
}

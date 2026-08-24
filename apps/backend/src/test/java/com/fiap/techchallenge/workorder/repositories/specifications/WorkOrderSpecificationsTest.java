package com.fiap.techchallenge.workorder.repositories.specifications;

import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Each factory method is just a null-check branch: filter present -> build a predicate, filter
 * absent -> null (Specification's no-op). Mocked Criteria API is enough to prove both branches
 * without touching a real database.
 */
class WorkOrderSpecificationsTest {

    private final Root<WorkOrder> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void belongsToCustomerIsANoOpWhenCustomerIdIsNull() {
        Specification<WorkOrder> spec = WorkOrderSpecifications.belongsToCustomerId(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void belongsToCustomerFiltersByCustomerIdWhenPresent() {
        UUID customerId = UUID.randomUUID();
        Path<Object> customerPath = mockPath("customerId");
        when(cb.equal(customerPath, customerId)).thenReturn(predicate);

        Specification<WorkOrder> spec = WorkOrderSpecifications.belongsToCustomerId(customerId);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(customerPath, customerId);
    }

    @Test
    void ofVehicleIsANoOpWhenVehicleIdIsNull() {
        Specification<WorkOrder> spec = WorkOrderSpecifications.ofVehicleId(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void ofVehicleFiltersByVehicleIdWhenPresent() {
        UUID vehicleId = UUID.randomUUID();
        Path<Object> vehiclePath = mockPath("vehicleId");
        when(cb.equal(vehiclePath, vehicleId)).thenReturn(predicate);

        Specification<WorkOrder> spec = WorkOrderSpecifications.ofVehicleId(vehicleId);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(vehiclePath, vehicleId);
    }

    @Test
    void withMechanicIsANoOpWhenMechanicIdIsNull() {
        Specification<WorkOrder> spec = WorkOrderSpecifications.withMechanicId(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void withMechanicFiltersByMechanicIdWhenPresent() {
        UUID mechanicId = UUID.randomUUID();
        Path<Object> mechanicPath = mockPath("mechanicId");
        when(cb.equal(mechanicPath, mechanicId)).thenReturn(predicate);

        Specification<WorkOrder> spec = WorkOrderSpecifications.withMechanicId(mechanicId);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(mechanicPath, mechanicId);
    }

    @Test
    void withStatusIsANoOpWhenStatusIsNull() {
        Specification<WorkOrder> spec = WorkOrderSpecifications.withStatus(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void withStatusIsANoOpWhenStatusesIsEmpty() {
        Specification<WorkOrder> spec = WorkOrderSpecifications.withStatus(Set.of());

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void withStatusFiltersByStatusWhenPresent() {
        Path<WorkOrderStatus> statusPath = mock();
        when(root.<WorkOrderStatus>get("status")).thenReturn(statusPath);
        when(statusPath.in(Set.of(WorkOrderStatus.IN_PROGRESS))).thenReturn(predicate);

        Specification<WorkOrder> spec = WorkOrderSpecifications.withStatus(Set.of(WorkOrderStatus.IN_PROGRESS));

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
    }

    @Test
    void withCodeIsANoOpWhenCodeIsNull() {
        Specification<WorkOrder> spec = WorkOrderSpecifications.withCode(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void withCodeFiltersByCodeWhenPresent() {
        Path<String> codePath = mock();
        Expression<String> lowerCodeExpr = mock();
        when(root.<String>get("orderCode")).thenReturn(codePath);
        when(cb.lower(codePath)).thenReturn(lowerCodeExpr);
        when(cb.like(lowerCodeExpr, "%wo-1%")).thenReturn(predicate);

        Specification<WorkOrder> spec = WorkOrderSpecifications.withCode("WO-1");

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).like(lowerCodeExpr, "%wo-1%");
    }

    @Test
    void createdBetweenIsANoOpWhenBothBoundsAreNull() {
        Specification<WorkOrder> spec = WorkOrderSpecifications.createdBetween(null, null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void createdBetweenFiltersUpToEndWhenStartIsNull() {
        LocalDate end = LocalDate.of(2026, 3, 10);
        Path<LocalDate> createdAtPath = mock();
        when(root.<LocalDate>get("createdAt")).thenReturn(createdAtPath);
        when(cb.lessThanOrEqualTo(createdAtPath, end)).thenReturn(predicate);

        Specification<WorkOrder> spec = WorkOrderSpecifications.createdBetween(null, end);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).lessThanOrEqualTo(createdAtPath, end);
    }

    @Test
    void createdBetweenFiltersFromStartWhenEndIsNull() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        Path<LocalDate> createdAtPath = mock();
        when(root.<LocalDate>get("createdAt")).thenReturn(createdAtPath);
        when(cb.greaterThanOrEqualTo(createdAtPath, start)).thenReturn(predicate);

        Specification<WorkOrder> spec = WorkOrderSpecifications.createdBetween(start, null);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).greaterThanOrEqualTo(createdAtPath, start);
    }

    @Test
    void createdBetweenFiltersBetweenBothBoundsWhenBothPresent() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 10);
        Path<LocalDate> createdAtPath = mock();
        when(root.<LocalDate>get("createdAt")).thenReturn(createdAtPath);
        when(cb.between(createdAtPath, start, end)).thenReturn(predicate);

        Specification<WorkOrder> spec = WorkOrderSpecifications.createdBetween(start, end);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).between(createdAtPath, start, end);
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

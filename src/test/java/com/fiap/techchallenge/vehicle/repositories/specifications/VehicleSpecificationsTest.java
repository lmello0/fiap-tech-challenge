package com.fiap.techchallenge.vehicle.repositories.specifications;

import com.fiap.techchallenge.vehicle.entities.Vehicle;
import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Each factory method is a null-check branch (the "between" ones have four: both null, start-only,
 * end-only, both present). Mocked Criteria API is enough to prove every branch without a database.
 *
 * <p>The {@code xIn} methods build their predicate as {@code cb.in(path.in(values))} —
 * {@code Expression.in(Collection)} returns a {@link Predicate}, not an {@code Expression<T>}, which
 * is why the intermediate mock below is typed {@code Predicate}, not the element type.
 */
class VehicleSpecificationsTest {

    private final Root<Vehicle> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void hasCustomerIdIsANoOpWhenNull() {
        assertThat(VehicleSpecifications.hasCustomerId(null).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void hasCustomerIdFiltersWhenPresent() {
        UUID customerId = UUID.randomUUID();
        Path<Object> path = mockPath("customerId");
        when(cb.equal(path, customerId)).thenReturn(predicate);

        assertThat(VehicleSpecifications.hasCustomerId(customerId).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(path, customerId);
    }

    @Test
    void typeInIsANoOpWhenNullOrEmpty() {
        assertThat(VehicleSpecifications.typeIn(null).toPredicate(root, query, cb)).isNull();
        assertThat(VehicleSpecifications.typeIn(List.of()).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void typeInFiltersWhenPresent() {
        Path<VehicleType> path = mock();
        Predicate innerPredicate = mock();
        CriteriaBuilder.In<Boolean> inPredicate = mock();
        when(root.<VehicleType>get("vehicleType")).thenReturn(path);
        when(path.in(List.of(VehicleType.CAR))).thenReturn(innerPredicate);
        when(cb.in(innerPredicate)).thenReturn(inPredicate);

        assertThat(VehicleSpecifications.typeIn(List.of(VehicleType.CAR)).toPredicate(root, query, cb)).isEqualTo(inPredicate);
        verify(cb).in(innerPredicate);
    }

    @Test
    void licensePlateEqualsIsANoOpWhenNull() {
        assertThat(VehicleSpecifications.licensePlateEquals(null).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void licensePlateEqualsFiltersWhenPresent() {
        Path<Object> path = mockPath("licensePlate");
        when(cb.equal(path, "AAA1111")).thenReturn(predicate);

        assertThat(VehicleSpecifications.licensePlateEquals("AAA1111").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(path, "AAA1111");
    }

    @Test
    void makeEqualsIsANoOpWhenNull() {
        assertThat(VehicleSpecifications.makeEquals(null).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void makeEqualsFiltersWhenPresent() {
        Path<Object> path = mockPath("make");
        when(cb.equal(path, "Toyota")).thenReturn(predicate);

        assertThat(VehicleSpecifications.makeEquals("Toyota").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(path, "Toyota");
    }

    @Test
    void modelEqualsIsANoOpWhenNull() {
        assertThat(VehicleSpecifications.modelEquals(null).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void modelEqualsFiltersWhenPresent() {
        Path<Object> path = mockPath("model");
        when(cb.equal(path, "Corolla")).thenReturn(predicate);

        assertThat(VehicleSpecifications.modelEquals("Corolla").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(path, "Corolla");
    }

    @Test
    void colorEqualsIsANoOpWhenNull() {
        assertThat(VehicleSpecifications.colorEquals(null).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void colorEqualsFiltersWhenPresent() {
        Path<Object> path = mockPath("color");
        when(cb.equal(path, "Black")).thenReturn(predicate);

        assertThat(VehicleSpecifications.colorEquals("Black").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(path, "Black");
    }

    @Test
    void modelYearBetweenIsANoOpWhenBothNull() {
        assertThat(VehicleSpecifications.modelYearBetween(null, null).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void modelYearBetweenFiltersUpToEndWhenStartIsNull() {
        Path<Integer> path = mockIntPath("modelYear");
        when(cb.lessThanOrEqualTo(path, 2023)).thenReturn(predicate);

        assertThat(VehicleSpecifications.modelYearBetween(null, 2023).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).lessThanOrEqualTo(path, 2023);
    }

    @Test
    void modelYearBetweenFiltersFromStartWhenEndIsNull() {
        Path<Integer> path = mockIntPath("modelYear");
        when(cb.greaterThanOrEqualTo(path, 2020)).thenReturn(predicate);

        assertThat(VehicleSpecifications.modelYearBetween(2020, null).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).greaterThanOrEqualTo(path, 2020);
    }

    @Test
    void modelYearBetweenFiltersBetweenBothBoundsWhenBothPresent() {
        Path<Integer> path = mockIntPath("modelYear");
        when(cb.between(path, 2020, 2023)).thenReturn(predicate);

        assertThat(VehicleSpecifications.modelYearBetween(2020, 2023).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).between(path, 2020, 2023);
    }

    @Test
    void manufactureYearBetweenIsANoOpWhenBothNull() {
        assertThat(VehicleSpecifications.manufactureYearBetween(null, null).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void manufactureYearBetweenFiltersUpToEndWhenStartIsNull() {
        Path<Integer> path = mockIntPath("manufactureYear");
        when(cb.lessThanOrEqualTo(path, 2023)).thenReturn(predicate);

        assertThat(VehicleSpecifications.manufactureYearBetween(null, 2023).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).lessThanOrEqualTo(path, 2023);
    }

    @Test
    void manufactureYearBetweenFiltersFromStartWhenEndIsNull() {
        Path<Integer> path = mockIntPath("manufactureYear");
        when(cb.greaterThanOrEqualTo(path, 2020)).thenReturn(predicate);

        assertThat(VehicleSpecifications.manufactureYearBetween(2020, null).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).greaterThanOrEqualTo(path, 2020);
    }

    @Test
    void manufactureYearBetweenFiltersBetweenBothBoundsWhenBothPresent() {
        Path<Integer> path = mockIntPath("manufactureYear");
        when(cb.between(path, 2020, 2023)).thenReturn(predicate);

        assertThat(VehicleSpecifications.manufactureYearBetween(2020, 2023).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).between(path, 2020, 2023);
    }

    @Test
    void fuelTypeInIsANoOpWhenNullOrEmpty() {
        assertThat(VehicleSpecifications.fuelTypeIn(null).toPredicate(root, query, cb)).isNull();
        assertThat(VehicleSpecifications.fuelTypeIn(List.of()).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void fuelTypeInFiltersWhenPresent() {
        Path<FuelType> path = mock();
        Predicate innerPredicate = mock();
        CriteriaBuilder.In<Boolean> inPredicate = mock();
        when(root.<FuelType>get("fuelType")).thenReturn(path);
        when(path.in(List.of(FuelType.FLEX))).thenReturn(innerPredicate);
        when(cb.in(innerPredicate)).thenReturn(inPredicate);

        assertThat(VehicleSpecifications.fuelTypeIn(List.of(FuelType.FLEX)).toPredicate(root, query, cb)).isEqualTo(inPredicate);
        verify(cb).in(innerPredicate);
    }

    @Test
    void transmissionTypeInIsANoOpWhenNullOrEmpty() {
        assertThat(VehicleSpecifications.transmissionTypeIn(null).toPredicate(root, query, cb)).isNull();
        assertThat(VehicleSpecifications.transmissionTypeIn(List.of()).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void transmissionTypeInFiltersWhenPresent() {
        Path<TransmissionType> path = mock();
        Predicate innerPredicate = mock();
        CriteriaBuilder.In<Boolean> inPredicate = mock();
        when(root.<TransmissionType>get("transmissionType")).thenReturn(path);
        when(path.in(List.of(TransmissionType.AUTOMATIC))).thenReturn(innerPredicate);
        when(cb.in(innerPredicate)).thenReturn(inPredicate);

        assertThat(VehicleSpecifications.transmissionTypeIn(List.of(TransmissionType.AUTOMATIC)).toPredicate(root, query, cb)).isEqualTo(inPredicate);
        verify(cb).in(innerPredicate);
    }

    @Test
    void createdBetweenIsANoOpWhenBothNull() {
        assertThat(VehicleSpecifications.createdBetween(null, null).toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void createdBetweenFiltersUpToEndWhenStartIsNull() {
        LocalDate end = LocalDate.of(2026, 3, 10);
        Path<LocalDate> path = mockDatePath("createdAt");
        when(cb.lessThanOrEqualTo(path, end)).thenReturn(predicate);

        assertThat(VehicleSpecifications.createdBetween(null, end).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).lessThanOrEqualTo(path, end);
    }

    @Test
    void createdBetweenFiltersFromStartWhenEndIsNull() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        Path<LocalDate> path = mockDatePath("createdAt");
        when(cb.greaterThanOrEqualTo(path, start)).thenReturn(predicate);

        assertThat(VehicleSpecifications.createdBetween(start, null).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).greaterThanOrEqualTo(path, start);
    }

    @Test
    void createdBetweenFiltersBetweenBothBoundsWhenBothPresent() {
        LocalDate start = LocalDate.of(2026, 3, 1);
        LocalDate end = LocalDate.of(2026, 3, 10);
        Path<LocalDate> path = mockDatePath("createdAt");
        when(cb.between(path, start, end)).thenReturn(predicate);

        assertThat(VehicleSpecifications.createdBetween(start, end).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).between(path, start, end);
    }

    private Path<Object> mockPath(String attribute) {
        Path<Object> path = mock();
        when(root.<Object>get(attribute)).thenReturn(path);
        return path;
    }

    private Path<Integer> mockIntPath(String attribute) {
        Path<Integer> path = mock();
        when(root.<Integer>get(attribute)).thenReturn(path);
        return path;
    }

    private Path<LocalDate> mockDatePath(String attribute) {
        Path<LocalDate> path = mock();
        when(root.<LocalDate>get(attribute)).thenReturn(path);
        return path;
    }
}

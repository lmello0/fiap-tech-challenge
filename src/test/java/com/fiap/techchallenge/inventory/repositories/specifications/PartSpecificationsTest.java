package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.Part;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Each factory method is just a null/empty-check branch: filter present -> build a predicate, filter
 * absent -> null (Specification's no-op). Mocked Criteria API is enough to prove both branches
 * without touching a real database.
 */
class PartSpecificationsTest {

    private final Root<Part> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void skuEqualsIsANoOpWhenSkuIsNull() {
        assertThat(PartSpecifications.skuEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void skuEqualsFiltersBySkuWhenPresent() {
        Path<Object> skuPath = mockPath("sku");
        when(cb.equal(skuPath, "SKU-1")).thenReturn(predicate);

        assertThat(PartSpecifications.skuEquals("SKU-1").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(skuPath, "SKU-1");
    }

    @Test
    void nameContainsIsANoOpWhenNameIsNull() {
        assertThat(PartSpecifications.nameContains(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void nameContainsFiltersCaseInsensitivelyWhenPresent() {
        Path<String> namePath = mock();
        when(root.<String>get("name")).thenReturn(namePath);
        Expression<String> loweredName = mock();
        when(cb.lower(namePath)).thenReturn(loweredName);
        when(cb.like(loweredName, "%brake%")).thenReturn(predicate);

        assertThat(PartSpecifications.nameContains("Brake").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).like(loweredName, "%brake%");
    }

    @Test
    void brandEqualsIsANoOpWhenBrandIsNull() {
        assertThat(PartSpecifications.brandEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void brandEqualsFiltersByBrandWhenPresent() {
        Path<Object> brandPath = mockPath("brand");
        when(cb.equal(brandPath, "Bosch")).thenReturn(predicate);

        assertThat(PartSpecifications.brandEquals("Bosch").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(brandPath, "Bosch");
    }

    @Test
    void unitOfMeasureInIsANoOpWhenListIsNull() {
        assertThat(PartSpecifications.unitOfMeasureIn(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void unitOfMeasureInIsANoOpWhenListIsEmpty() {
        assertThat(PartSpecifications.unitOfMeasureIn(List.of()).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void unitOfMeasureInFiltersByListWhenPresent() {
        // root.get("unitOfMeasure").in(list) builds a Predicate; cb.in(that Predicate) then wraps it
        // in a CriteriaBuilder.In (itself a Predicate, since Predicate extends Expression<Boolean>).
        Path<UnitOfMeasure> uomPath = mock();
        when(root.<UnitOfMeasure>get("unitOfMeasure")).thenReturn(uomPath);

        Predicate uomInList = mock();
        when(uomPath.in(List.of(UnitOfMeasure.UNIT, UnitOfMeasure.LITER))).thenReturn(uomInList);

        CriteriaBuilder.In<Boolean> inClause = mock();
        when(cb.in(uomInList)).thenReturn(inClause);

        assertThat(PartSpecifications.unitOfMeasureIn(List.of(UnitOfMeasure.UNIT, UnitOfMeasure.LITER))
                .toPredicate(root, query, cb)).isEqualTo(inClause);
        verify(cb).in(uomInList);
    }

    @Test
    void activeEqualsIsANoOpWhenActiveIsNull() {
        assertThat(PartSpecifications.activeEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void activeEqualsFiltersByActiveWhenPresent() {
        Path<Object> activePath = mockPath("active");
        when(cb.equal(activePath, false)).thenReturn(predicate);

        assertThat(PartSpecifications.activeEquals(false).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(activePath, false);
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

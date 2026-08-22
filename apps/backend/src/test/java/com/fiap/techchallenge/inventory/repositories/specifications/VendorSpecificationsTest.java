package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.Vendor;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Each factory method is just a null-check branch: filter present -> build a predicate, filter
 * absent -> null (Specification's no-op). Mocked Criteria API is enough to prove both branches
 * without touching a real database.
 */
class VendorSpecificationsTest {

    private final Root<Vendor> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void nameContainsIsANoOpWhenNameIsNull() {
        Specification<Vendor> spec = VendorSpecifications.nameContains(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void nameContainsFiltersCaseInsensitivelyWhenPresent() {
        Path<String> namePath = mock();
        when(root.<String>get("name")).thenReturn(namePath);
        Expression<String> loweredName = mock();
        when(cb.lower(namePath)).thenReturn(loweredName);
        when(cb.like(loweredName, "%acme%")).thenReturn(predicate);

        Specification<Vendor> spec = VendorSpecifications.nameContains("Acme");

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).like(loweredName, "%acme%");
    }

    @Test
    void activeEqualsIsANoOpWhenActiveIsNull() {
        Specification<Vendor> spec = VendorSpecifications.activeEquals(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void activeEqualsFiltersByActiveWhenPresent() {
        Path<Object> activePath = mockPath("active");
        when(cb.equal(activePath, true)).thenReturn(predicate);

        Specification<Vendor> spec = VendorSpecifications.activeEquals(true);

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(activePath, true);
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

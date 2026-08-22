package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.ReorderRule;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReorderRuleSpecificationsTest {

    private final Root<ReorderRule> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void partIdEqualsIsANoOpWhenPartIdIsNull() {
        assertThat(ReorderRuleSpecifications.partIdEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void partIdEqualsFiltersByNestedPartIdWhenPresent() {
        UUID partId = UUID.randomUUID();
        Path<Object> partPath = mockPath(root, "part");
        Path<Object> partIdPath = mockPath(partPath, "id");
        when(cb.equal(partIdPath, partId)).thenReturn(predicate);

        assertThat(ReorderRuleSpecifications.partIdEquals(partId).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(partIdPath, partId);
    }

    @Test
    void vendorIdEqualsIsANoOpWhenVendorIdIsNull() {
        assertThat(ReorderRuleSpecifications.vendorIdEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void vendorIdEqualsFiltersByNestedVendorIdWhenPresent() {
        UUID vendorId = UUID.randomUUID();
        Path<Object> vendorPath = mockPath(root, "vendor");
        Path<Object> vendorIdPath = mockPath(vendorPath, "id");
        when(cb.equal(vendorIdPath, vendorId)).thenReturn(predicate);

        assertThat(ReorderRuleSpecifications.vendorIdEquals(vendorId).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(vendorIdPath, vendorId);
    }

    @Test
    void enabledEqualsIsANoOpWhenEnabledIsNull() {
        assertThat(ReorderRuleSpecifications.enabledEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void enabledEqualsFiltersByEnabledWhenPresent() {
        Path<Object> enabledPath = mockPath(root, "enabled");
        when(cb.equal(enabledPath, true)).thenReturn(predicate);

        assertThat(ReorderRuleSpecifications.enabledEquals(true).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(enabledPath, true);
    }

    private Path<Object> mockPath(Path<?> parent, String attribute) {
        Path<Object> path = mock();
        when(parent.<Object>get(attribute)).thenReturn(path);
        return path;
    }

    private void verifyNoInteractionsWithCriteriaBuilder() {
        Mockito.verifyNoInteractions(cb);
    }
}

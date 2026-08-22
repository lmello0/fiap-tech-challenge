package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.RepairService;
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

class RepairServiceSpecificationsTest {

    private final Root<RepairService> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void codeEqualsIsANoOpWhenCodeIsNull() {
        assertThat(RepairServiceSpecifications.codeEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void codeEqualsFiltersByCodeWhenPresent() {
        Path<Object> codePath = mockPath("code");
        when(cb.equal(codePath, "BRK-001")).thenReturn(predicate);

        assertThat(RepairServiceSpecifications.codeEquals("BRK-001").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(codePath, "BRK-001");
    }

    @Test
    void nameContainsIsANoOpWhenNameIsNull() {
        assertThat(RepairServiceSpecifications.nameContains(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void nameContainsFiltersCaseInsensitivelyWhenPresent() {
        Path<String> namePath = mock();
        when(root.<String>get("name")).thenReturn(namePath);
        Expression<String> loweredName = mock();
        when(cb.lower(namePath)).thenReturn(loweredName);
        when(cb.like(loweredName, "%pad%")).thenReturn(predicate);

        assertThat(RepairServiceSpecifications.nameContains("Pad").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).like(loweredName, "%pad%");
    }

    @Test
    void activeEqualsIsANoOpWhenActiveIsNull() {
        assertThat(RepairServiceSpecifications.activeEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void activeEqualsFiltersByActiveWhenPresent() {
        Path<Object> activePath = mockPath("active");
        when(cb.equal(activePath, true)).thenReturn(predicate);

        assertThat(RepairServiceSpecifications.activeEquals(true).toPredicate(root, query, cb)).isEqualTo(predicate);
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

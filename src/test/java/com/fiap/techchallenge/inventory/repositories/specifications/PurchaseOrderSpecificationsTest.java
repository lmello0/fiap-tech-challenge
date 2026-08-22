package com.fiap.techchallenge.inventory.repositories.specifications;

import com.fiap.techchallenge.inventory.entities.PurchaseOrder;
import com.fiap.techchallenge.inventory.enums.PurchaseOrderStatus;
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

class PurchaseOrderSpecificationsTest {

    private final Root<PurchaseOrder> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void vendorIdEqualsIsANoOpWhenVendorIdIsNull() {
        assertThat(PurchaseOrderSpecifications.vendorIdEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void vendorIdEqualsFiltersByNestedVendorIdWhenPresent() {
        UUID vendorId = UUID.randomUUID();
        Path<Object> vendorPath = mockPath(root, "vendor");
        Path<Object> vendorIdPath = mockPath(vendorPath, "id");
        when(cb.equal(vendorIdPath, vendorId)).thenReturn(predicate);

        assertThat(PurchaseOrderSpecifications.vendorIdEquals(vendorId).toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(vendorIdPath, vendorId);
    }

    @Test
    void statusEqualsIsANoOpWhenStatusIsNull() {
        assertThat(PurchaseOrderSpecifications.statusEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void statusEqualsFiltersByStatusWhenPresent() {
        Path<Object> statusPath = mockPath(root, "status");
        when(cb.equal(statusPath, PurchaseOrderStatus.RECEIVED)).thenReturn(predicate);

        assertThat(PurchaseOrderSpecifications.statusEquals(PurchaseOrderStatus.RECEIVED).toPredicate(root, query, cb))
                .isEqualTo(predicate);
        verify(cb).equal(statusPath, PurchaseOrderStatus.RECEIVED);
    }

    @Test
    void codeEqualsIsANoOpWhenCodeIsNull() {
        assertThat(PurchaseOrderSpecifications.codeEquals(null).toPredicate(root, query, cb)).isNull();
        verifyNoInteractionsWithCriteriaBuilder();
    }

    @Test
    void codeEqualsFiltersByCodeWhenPresent() {
        Path<Object> codePath = mockPath(root, "code");
        when(cb.equal(codePath, "PO-001")).thenReturn(predicate);

        assertThat(PurchaseOrderSpecifications.codeEquals("PO-001").toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).equal(codePath, "PO-001");
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

package com.fiap.techchallenge.user.repositories.specifications;

import com.fiap.techchallenge.user.entities.PhoneNumber;
import com.fiap.techchallenge.user.entities.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
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
 * Each factory method is a null/blank-check branch: filter present -> build a predicate, filter
 * absent -> null (Specification's no-op). Mocked Criteria API is enough to prove both branches
 * without touching a real database.
 */
class UserSpecificationsTest {

    private final Root<User> root = mock();
    private final CriteriaQuery<?> query = mock();
    private final CriteriaBuilder cb = mock();
    private final Predicate predicate = mock();

    @Test
    void isCustomerJoinsOnTheCustomerFacetAndMarksTheQueryDistinct() {
        Join<Object, Object> join = mock();
        when(root.<Object, Object>join("customer", JoinType.INNER)).thenReturn(join);
        when(cb.isNotNull(join)).thenReturn(predicate);

        Specification<User> spec = UserSpecifications.isCustomer();

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(query).distinct(true);
    }

    @Test
    void isWorkerJoinsOnTheWorkerFacetAndMarksTheQueryDistinct() {
        Join<Object, Object> join = mock();
        when(root.<Object, Object>join("worker", JoinType.INNER)).thenReturn(join);
        when(cb.isNotNull(join)).thenReturn(predicate);

        Specification<User> spec = UserSpecifications.isWorker();

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(query).distinct(true);
    }

    @Test
    void nameContainsIsANoOpWhenBlank() {
        Specification<User> spec = UserSpecifications.nameContains("   ");

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void nameContainsFiltersOnFirstAndLastNameWhenPresent() {
        Path<String> firstName = mock();
        Path<String> lastName = mock();
        Expression<String> concatSpace = mock();
        Expression<String> coalescedLast = mock();
        Expression<String> fullConcat = mock();
        Expression<String> lowered = mock();

        when(root.<String>get("firstName")).thenReturn(firstName);
        when(root.<String>get("lastName")).thenReturn(lastName);
        when(cb.concat(firstName, " ")).thenReturn(concatSpace);
        when(cb.coalesce(lastName, "")).thenReturn(coalescedLast);
        when(cb.concat(concatSpace, coalescedLast)).thenReturn(fullConcat);
        when(cb.lower(fullConcat)).thenReturn(lowered);
        when(cb.like(lowered, "%ana%")).thenReturn(predicate);

        Specification<User> spec = UserSpecifications.nameContains("Ana");

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).like(lowered, "%ana%");
    }

    @Test
    void emailContainsIsANoOpWhenBlank() {
        Specification<User> spec = UserSpecifications.emailContains("");

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void emailContainsFiltersCaseInsensitivelyWhenPresent() {
        Path<String> emailPath = mock();
        Expression<String> lowered = mock();

        when(root.<String>get("email")).thenReturn(emailPath);
        when(cb.lower(emailPath)).thenReturn(lowered);
        when(cb.like(lowered, "%ana@example.com%")).thenReturn(predicate);

        Specification<User> spec = UserSpecifications.emailContains("Ana@Example.com");

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).like(lowered, "%ana@example.com%");
    }

    @Test
    void documentContainsIsANoOpWhenBlank() {
        Specification<User> spec = UserSpecifications.documentContains(null);

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void documentContainsFiltersWhenPresent() {
        Path<String> documentPath = mock();
        when(root.<String>get("documentCode")).thenReturn(documentPath);
        when(cb.like(documentPath, "%529982%")).thenReturn(predicate);

        Specification<User> spec = UserSpecifications.documentContains("529982");

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(cb).like(documentPath, "%529982%");
    }

    @Test
    void phoneContainsIsANoOpWhenBlank() {
        Specification<User> spec = UserSpecifications.phoneContains(" ");

        assertThat(spec.toPredicate(root, query, cb)).isNull();
        Mockito.verifyNoInteractions(cb);
    }

    @Test
    void phoneContainsJoinsPhoneNumbersAndMarksTheQueryDistinctWhenPresent() {
        Join<User, PhoneNumber> phones = mock();
        Path<String> phonePath = mock();

        when(root.<User, PhoneNumber>join("phoneNumbers", JoinType.LEFT)).thenReturn(phones);
        when(phones.<String>get("phone")).thenReturn(phonePath);
        when(cb.like(phonePath, "%11999%")).thenReturn(predicate);

        Specification<User> spec = UserSpecifications.phoneContains("11999");

        assertThat(spec.toPredicate(root, query, cb)).isEqualTo(predicate);
        verify(query).distinct(true);
        verify(cb).like(phonePath, "%11999%");
    }
}

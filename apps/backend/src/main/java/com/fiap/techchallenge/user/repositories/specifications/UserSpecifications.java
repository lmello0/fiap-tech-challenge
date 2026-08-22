package com.fiap.techchallenge.user.repositories.specifications;

import com.fiap.techchallenge.user.entities.PhoneNumber;
import com.fiap.techchallenge.user.entities.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecifications {

    public static Specification<User> isCustomer() {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.isNotNull(root.join("customer", JoinType.INNER));
        };
    }

    public static Specification<User> isWorker() {
        return (root, query, cb) -> {
            query.distinct(true);
            return cb.isNotNull(root.join("worker", JoinType.INNER));
        };
    }

    public static Specification<User> nameContains(String name) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(name)) {
                return null;
            }

            return cb.like(
                    cb.lower(cb.concat(cb.concat(root.get("firstName"), " "), cb.coalesce(root.get("lastName"), ""))),
                    "%" + name.toLowerCase() + "%"
            );
        };
    }

    public static Specification<User> emailContains(String email) {
        return (root, query, cb) ->
                StringUtils.hasText(email) ? cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%") : null;
    }

    public static Specification<User> documentContains(String document) {
        return (root, query, cb) ->
                StringUtils.hasText(document) ? cb.like(root.get("documentCode"), "%" + document + "%") : null;
    }

    public static Specification<User> phoneContains(String phone) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(phone)) {
                return null;
            }

            query.distinct(true);
            Join<User, PhoneNumber> phones = root.join("phoneNumbers", JoinType.LEFT);

            return cb.like(phones.get("phone"), "%" + phone + "%");
        };
    }
}

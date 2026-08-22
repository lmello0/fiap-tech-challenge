package com.fiap.techchallenge.user.repositories;

import com.fiap.techchallenge.user.api.representation.UserPrincipal;
import com.fiap.techchallenge.user.entities.User;
import com.fiap.techchallenge.user.enums.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByDocumentTypeAndDocumentCode(DocumentType documentType, String documentCode);

    @Query("""
            SELECT new com.fiap.techchallenge.user.api.representation.UserPrincipal(
                u.id,
                u.email,
                u.emailVerified,
                c.userId IS NOT NULL,
                COALESCE(c.active, false),
                w.userId IS NOT NULL,
                COALESCE(w.isActive, false),
                w.role)
            FROM User u
            LEFT JOIN u.customer c
            LEFT JOIN u.worker w
            WHERE u.email = :email
            """)
    Optional<UserPrincipal> findPrincipalByEmail(@Param("email") String email);

    @Query("""
            SELECT new com.fiap.techchallenge.user.api.representation.UserPrincipal(
                u.id,
                u.email,
                u.emailVerified,
                c.userId IS NOT NULL,
                COALESCE(c.active, false),
                w.userId IS NOT NULL,
                COALESCE(w.isActive, false),
                w.role)
            FROM User u
            LEFT JOIN u.customer c
            LEFT JOIN u.worker w
            WHERE u.id = :id
            """)
    Optional<UserPrincipal> findPrincipalById(@Param("id") UUID id);
}

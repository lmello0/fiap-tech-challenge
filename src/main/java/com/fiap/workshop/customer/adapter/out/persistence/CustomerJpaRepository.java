package com.fiap.workshop.customer.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface CustomerJpaRepository extends JpaRepository<CustomerEntity, UUID>, JpaSpecificationExecutor<CustomerEntity> {

	Optional<CustomerEntity> findByDocument(String document);

	boolean existsByDocument(String document);

	boolean existsByEmail(String email);

	boolean existsByEmailAndIdNot(String email, UUID id);
}

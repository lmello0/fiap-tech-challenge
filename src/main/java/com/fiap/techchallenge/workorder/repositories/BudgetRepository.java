package com.fiap.techchallenge.workorder.repositories;

import com.fiap.techchallenge.workorder.entities.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository extends JpaRepository<Budget, UUID> {

    @Query("select b from Budget b left join fetch b.lines where b.id = :id")
    Optional<Budget> findWithLinesById(UUID id);
}

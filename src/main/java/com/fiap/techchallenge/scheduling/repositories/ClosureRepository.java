package com.fiap.techchallenge.scheduling.repositories;

import com.fiap.techchallenge.scheduling.entities.Closure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClosureRepository extends JpaRepository<Closure, UUID> {

    boolean existsByDate(LocalDate date);

    Optional<Closure> findByDate(LocalDate date);

    List<Closure> findAllByOrderByDateAsc();
}

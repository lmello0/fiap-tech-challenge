package com.fiap.techchallenge.user.repositories;

import com.fiap.techchallenge.user.entities.Worker;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.UUID;

public interface WorkerRepository extends JpaRepository<Worker, UUID> {

    boolean existsByRegistration(String registration);

    @Query("SELECT nextval('users.seq_worker_registration')")
    Long getNextRegistrationSeq();
}

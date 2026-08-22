package com.fiap.techchallenge.workorder.schedules;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.workorder.repositories.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ResetWorkOrderSequenceTest {

    @Autowired
    ResetWorkOrderSequence job;

    @Autowired
    WorkOrderRepository repository;

    @Test
    void resetsTheSequenceBackToOne() {
        repository.getNextSequence();
        repository.getNextSequence();
        Long advanced = repository.getNextSequence();
        assertThat(advanced).isGreaterThan(1L);

        job.reset();

        assertThat(repository.getNextSequence()).isEqualTo(1L);
    }
}

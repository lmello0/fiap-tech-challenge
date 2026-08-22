package com.fiap.techchallenge.workorder.exceptions;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorkOrderNotFoundExceptionTest {

    @Test
    void carriesTheIdWhenLookedUpById() {
        UUID id = UUID.randomUUID();

        WorkOrderNotFoundException exception = new WorkOrderNotFoundException(id);

        assertThat(exception.getMessage()).isEqualTo("Work order not found: " + id);
    }

    @Test
    void carriesTheCodeWhenLookedUpByCode() {
        WorkOrderNotFoundException exception = new WorkOrderNotFoundException("WO-1234");

        assertThat(exception.getMessage()).isEqualTo("Work order not found: WO-1234");
    }
}

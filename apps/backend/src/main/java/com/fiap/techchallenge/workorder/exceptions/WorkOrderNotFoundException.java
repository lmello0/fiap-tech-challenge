package com.fiap.techchallenge.workorder.exceptions;

import java.util.UUID;

public class WorkOrderNotFoundException extends RuntimeException {
    public WorkOrderNotFoundException(UUID id) {
        super("Work order not found: " + id);
    }

    public WorkOrderNotFoundException(String code) {
        super("Work order not found: " + code);
    }
}

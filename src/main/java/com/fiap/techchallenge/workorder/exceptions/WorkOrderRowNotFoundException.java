package com.fiap.techchallenge.workorder.exceptions;

import java.util.UUID;

public class WorkOrderRowNotFoundException extends RuntimeException {
    public WorkOrderRowNotFoundException(UUID id) {
        super("Work order row not found: " + id);
    }
}

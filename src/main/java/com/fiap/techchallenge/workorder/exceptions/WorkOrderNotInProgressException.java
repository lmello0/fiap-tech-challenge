package com.fiap.techchallenge.workorder.exceptions;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;

import java.util.UUID;

/** Thrown by row start/finish, which are only meaningful while the work order is IN_PROGRESS. */
public class WorkOrderNotInProgressException extends RuntimeException {
    public WorkOrderNotInProgressException(UUID workOrderId, WorkOrderStatus actual) {
        super("Work order " + workOrderId + " is " + actual + ", not IN_PROGRESS");
    }
}

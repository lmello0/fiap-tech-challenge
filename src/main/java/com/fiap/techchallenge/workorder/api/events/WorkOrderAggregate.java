package com.fiap.techchallenge.workorder.api.events;

/** The Timeline (CONTEXT.md) every Work Order, Budget, and Budget Line event belongs to. */
public final class WorkOrderAggregate {

    public static final String TYPE = "WORK_ORDER";

    private WorkOrderAggregate() {
    }
}

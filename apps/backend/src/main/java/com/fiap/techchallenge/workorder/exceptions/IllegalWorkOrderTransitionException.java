package com.fiap.techchallenge.workorder.exceptions;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import lombok.Getter;

@Getter
public class IllegalWorkOrderTransitionException extends RuntimeException {

    private final WorkOrderStatus from;
    private final WorkOrderStatus to;

    public IllegalWorkOrderTransitionException(WorkOrderStatus from, WorkOrderStatus to) {
        super("Illegal transition: " + from + " -> " + to);

        this.from = from;
        this.to = to;
    }
}

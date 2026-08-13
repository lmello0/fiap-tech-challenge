package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class WorkOrderStateMachine {
    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> TRANSITIONS = Map.of(
            WorkOrderStatus.RECEIVED, EnumSet.of(WorkOrderStatus.WAITING_DIAGNOSTICS),
            WorkOrderStatus.WAITING_DIAGNOSTICS, EnumSet.of(WorkOrderStatus.IN_DIAGNOSTICS),
            WorkOrderStatus.IN_DIAGNOSTICS, EnumSet.of(WorkOrderStatus.WAITING_APPROVAL),
            WorkOrderStatus.WAITING_APPROVAL, EnumSet.of(WorkOrderStatus.APPROVED, WorkOrderStatus.REFUSED),
            WorkOrderStatus.APPROVED, EnumSet.of(WorkOrderStatus.IN_PROGRESS),
            WorkOrderStatus.IN_PROGRESS, EnumSet.of(WorkOrderStatus.FINISHED),
            WorkOrderStatus.FINISHED, EnumSet.of(WorkOrderStatus.WAITING_PICKUP),
            WorkOrderStatus.REFUSED, EnumSet.of(WorkOrderStatus.WAITING_PICKUP),
            WorkOrderStatus.WAITING_PICKUP, EnumSet.of(WorkOrderStatus.DELIVERED),
            WorkOrderStatus.DELIVERED, EnumSet.noneOf(WorkOrderStatus.class)
    );

    public boolean canTransition(WorkOrderStatus from, WorkOrderStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(WorkOrderStatus.class)).contains(to);
    }

    public WorkOrderStatus transition(WorkOrderStatus from, WorkOrderStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalStateException("Illegal transition: " + from + " -> " + to);
        }

        return to;
    }

    public Set<WorkOrderStatus> allowedNext(WorkOrderStatus from) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(WorkOrderStatus.class));
    }

    public boolean isTerminal(WorkOrderStatus status) {
        return allowedNext(status).isEmpty();
    }
}

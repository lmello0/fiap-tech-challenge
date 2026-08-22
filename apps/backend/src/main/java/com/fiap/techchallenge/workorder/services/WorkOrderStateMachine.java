package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.exceptions.IllegalWorkOrderTransitionException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class WorkOrderStateMachine {
    private static final Map<WorkOrderStatus, Set<WorkOrderStatus>> TRANSITIONS = Map.ofEntries(
            Map.entry(WorkOrderStatus.RECEIVED, EnumSet.of(WorkOrderStatus.WAITING_DIAGNOSTICS)),
            Map.entry(WorkOrderStatus.WAITING_DIAGNOSTICS, EnumSet.of(WorkOrderStatus.IN_DIAGNOSTICS)),
            Map.entry(WorkOrderStatus.IN_DIAGNOSTICS, EnumSet.of(WorkOrderStatus.BUDGET_IN_DRAFT)),
            Map.entry(WorkOrderStatus.BUDGET_IN_DRAFT, EnumSet.of(WorkOrderStatus.WAITING_APPROVAL)),
            Map.entry(WorkOrderStatus.WAITING_APPROVAL, EnumSet.of(WorkOrderStatus.APPROVED, WorkOrderStatus.REFUSED)),
            Map.entry(WorkOrderStatus.APPROVED, EnumSet.of(WorkOrderStatus.IN_PROGRESS)),
            Map.entry(WorkOrderStatus.IN_PROGRESS, EnumSet.of(WorkOrderStatus.FINISHED)),
            Map.entry(WorkOrderStatus.FINISHED, EnumSet.of(WorkOrderStatus.WAITING_PICKUP)),
            Map.entry(WorkOrderStatus.REFUSED, EnumSet.of(WorkOrderStatus.WAITING_PICKUP)),
            Map.entry(WorkOrderStatus.WAITING_PICKUP, EnumSet.of(WorkOrderStatus.DELIVERED)),
            Map.entry(WorkOrderStatus.DELIVERED, EnumSet.noneOf(WorkOrderStatus.class))
    );

    public boolean canTransition(WorkOrderStatus from, WorkOrderStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(WorkOrderStatus.class)).contains(to);
    }

    public WorkOrderStatus transition(WorkOrderStatus from, WorkOrderStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalWorkOrderTransitionException(from, to);
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

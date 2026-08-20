package com.fiap.techchallenge.workorder.services;

import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import com.fiap.techchallenge.workorder.exceptions.IllegalBudgetTransitionException;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

@Service
public class BudgetStateMachine {
    private static final Map<BudgetStatus, Set<BudgetStatus>> TRANSITIONS = Map.of(
            BudgetStatus.DRAFT, EnumSet.of(BudgetStatus.WAITING_SEND),
            BudgetStatus.WAITING_SEND, EnumSet.of(BudgetStatus.SENT),
            BudgetStatus.SENT, EnumSet.of(BudgetStatus.APPROVED, BudgetStatus.REFUSED),
            BudgetStatus.APPROVED, EnumSet.noneOf(BudgetStatus.class),
            BudgetStatus.REFUSED, EnumSet.noneOf(BudgetStatus.class)
    );

    public boolean canTransition(BudgetStatus from, BudgetStatus to) {
        return TRANSITIONS.getOrDefault(from, EnumSet.noneOf(BudgetStatus.class)).contains(to);
    }

    public BudgetStatus transition(BudgetStatus from, BudgetStatus to) {
        if (!canTransition(from, to)) {
            throw new IllegalBudgetTransitionException(from, to);
        }

        return to;
    }
}

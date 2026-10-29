package com.fiap.workshop.workorder.domain;

import com.fiap.workshop.shared.domain.Actor;
import java.time.Instant;

public record Decision(Actor actor, DecisionChannel channel, String reason, Instant at) {
}

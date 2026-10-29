package com.fiap.workshop.workorder.domain;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Instant;
import java.util.UUID;

public record StatusChange(UUID id, WorkOrderStatus from, WorkOrderStatus to, Instant occurredAt, Actor actor, String note) {

	static StatusChange of(WorkOrderStatus from, WorkOrderStatus to, Actor actor, String note, Instant now) {
		return new StatusChange(UuidV7.generate(), from, to, now, actor, note);
	}
}

package com.fiap.workshop.workorder.application.service;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import java.util.UUID;

final class DecisionChannels {

	private DecisionChannels() {
	}

	static DecisionChannel resolve(UUID customerScope, Actor actor, DecisionChannel requested) {
		if (customerScope != null) {
			return DecisionChannel.APP;
		}
		if (Actor.EXTERNAL.role().equals(actor.role())) {
			return DecisionChannel.EXTERNAL;
		}
		if (requested != DecisionChannel.PHONE && requested != DecisionChannel.IN_PERSON) {
			throw new DomainException("Staff must record the channel the customer used: PHONE or IN_PERSON");
		}
		return requested;
	}
}

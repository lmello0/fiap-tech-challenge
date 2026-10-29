package com.fiap.workshop.workorder.application.port.out;

import java.util.UUID;

public interface ExternalDecisions {

	boolean firstTime(UUID eventId);
}

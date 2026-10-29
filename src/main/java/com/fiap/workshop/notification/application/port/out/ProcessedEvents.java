package com.fiap.workshop.notification.application.port.out;

import java.util.UUID;

public interface ProcessedEvents {

	boolean firstTime(UUID eventId, String listener);
}

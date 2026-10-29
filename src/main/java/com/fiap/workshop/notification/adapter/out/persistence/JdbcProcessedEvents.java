package com.fiap.workshop.notification.adapter.out.persistence;

import com.fiap.workshop.notification.application.port.out.ProcessedEvents;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcProcessedEvents implements ProcessedEvents {

	private final JdbcClient jdbc;
	private final Clock clock;

	JdbcProcessedEvents(JdbcClient jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Override
	public boolean firstTime(UUID eventId, String listener) {
		return jdbc.sql("insert into notification.processed_event (event_id, listener, processed_at) values (?, ?, ?) on conflict do nothing")
				.params(eventId, listener, java.sql.Timestamp.from(clock.instant()))
				.update() == 1;
	}
}

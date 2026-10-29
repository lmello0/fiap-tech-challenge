package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.application.port.out.ExternalDecisions;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
class JdbcExternalDecisions implements ExternalDecisions {

	private final JdbcClient jdbc;
	private final Clock clock;

	JdbcExternalDecisions(JdbcClient jdbc, Clock clock) {
		this.jdbc = jdbc;
		this.clock = clock;
	}

	@Override
	public boolean firstTime(UUID eventId) {
		return jdbc.sql("insert into workorder.external_decision (event_id, received_at) values (?, ?) on conflict do nothing")
				.params(eventId, Timestamp.from(clock.instant()))
				.update() == 1;
	}
}

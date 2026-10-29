package com.fiap.workshop.shared.application;

import java.util.ArrayList;
import java.util.List;

public class RecordingEventPublisher implements EventPublisher {

	private final List<Object> events = new ArrayList<>();

	@Override
	public void publish(Object event) {
		events.add(event);
	}

	public List<Object> events() {
		return events;
	}

	public <T> List<T> of(Class<T> type) {
		return events.stream().filter(type::isInstance).map(type::cast).toList();
	}
}

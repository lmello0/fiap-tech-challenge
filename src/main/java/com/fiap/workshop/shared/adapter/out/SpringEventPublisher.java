package com.fiap.workshop.shared.adapter.out;

import com.fiap.workshop.shared.application.EventPublisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
class SpringEventPublisher implements EventPublisher {

	private final ApplicationEventPublisher delegate;

	SpringEventPublisher(ApplicationEventPublisher delegate) {
		this.delegate = delegate;
	}

	@Override
	public void publish(Object event) {
		delegate.publishEvent(event);
	}
}

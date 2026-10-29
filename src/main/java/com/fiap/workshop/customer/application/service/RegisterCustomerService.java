package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.RegisterCustomerUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.Document;
import com.fiap.workshop.shared.domain.Email;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class RegisterCustomerService implements RegisterCustomerUseCase {

	private final CustomerRepository customers;
	private final EventPublisher events;
	private final Clock clock;

	RegisterCustomerService(CustomerRepository customers, EventPublisher events, Clock clock) {
		this.customers = customers;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Customer execute(Command command) {
		Document document = Document.of(command.document());
		Email email = Email.of(command.email());
		if (customers.existsByDocument(document)) {
			throw new ConflictException("A customer with this document already exists");
		}
		if (customers.existsByEmail(email)) {
			throw new ConflictException("A customer with this e-mail already exists");
		}
		Customer customer = customers.save(Customer.register(command.name(), document, email, command.phone(), clock.instant()));
		events.publish(CustomerEvents.registered(customer, clock.instant()));
		return customer;
	}
}

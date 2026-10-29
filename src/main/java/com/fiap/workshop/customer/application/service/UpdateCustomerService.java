package com.fiap.workshop.customer.application.service;

import com.fiap.workshop.customer.application.port.in.UpdateCustomerUseCase;
import com.fiap.workshop.customer.application.port.out.CustomerRepository;
import com.fiap.workshop.customer.domain.Customer;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.Email;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class UpdateCustomerService implements UpdateCustomerUseCase {

	private final CustomerRepository customers;
	private final EventPublisher events;
	private final Clock clock;

	UpdateCustomerService(CustomerRepository customers, EventPublisher events, Clock clock) {
		this.customers = customers;
		this.events = events;
		this.clock = clock;
	}

	@Override
	@Transactional
	public Customer execute(Command command) {
		Customer customer = customers.findById(command.id())
				.orElseThrow(() -> new NotFoundException("Customer " + command.id() + " not found"));
		Email email = Email.of(command.email());
		if (customers.existsByEmailAndIdNot(email, customer.id())) {
			throw new ConflictException("A customer with this e-mail already exists");
		}
		customer.update(command.name(), email, command.phone(), clock.instant());
		customers.save(customer);
		events.publish(CustomerEvents.updated(customer, clock.instant()));
		return customer;
	}
}

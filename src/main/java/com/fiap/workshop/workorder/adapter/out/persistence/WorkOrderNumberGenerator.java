package com.fiap.workshop.workorder.adapter.out.persistence;

import com.fiap.workshop.workorder.application.port.out.WorkOrderNumbers;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

@Component
class WorkOrderNumberGenerator implements WorkOrderNumbers {

	@PersistenceContext
	private EntityManager entityManager;

	@Override
	public String next(int year) {
		long sequence = ((Number) entityManager.createNativeQuery("select nextval('workorder.work_order_number_seq')").getSingleResult()).longValue();
		return "OS-%d-%06d".formatted(year, sequence);
	}
}

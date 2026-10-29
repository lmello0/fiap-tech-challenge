package com.fiap.workshop.inventory.application.port.out;

import com.fiap.workshop.inventory.domain.StockReservation;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockReservationRepository {

	StockReservation save(StockReservation reservation);

	Optional<StockReservation> findByBudgetId(UUID budgetId);

	List<StockReservation> findByWorkOrderId(UUID workOrderId);

	List<StockReservation> findWaitingForParts(Collection<UUID> partIds);
}

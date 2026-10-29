package com.fiap.workshop.inventory.application.service;

import com.fiap.workshop.inventory.api.events.PartsReserved;
import com.fiap.workshop.inventory.api.events.PartsShortage;
import com.fiap.workshop.inventory.api.events.ReservationConsumed;
import com.fiap.workshop.inventory.api.events.ReservationReleased;
import com.fiap.workshop.inventory.api.events.ShortagePart;
import com.fiap.workshop.inventory.application.port.in.PartQuantity;
import com.fiap.workshop.inventory.application.port.out.PartRepository;
import com.fiap.workshop.inventory.application.port.out.StockMovementRepository;
import com.fiap.workshop.inventory.application.port.out.StockReservationRepository;
import com.fiap.workshop.inventory.domain.Part;
import com.fiap.workshop.inventory.domain.ReservationLine;
import com.fiap.workshop.inventory.domain.ReservationStatus;
import com.fiap.workshop.inventory.domain.StockReservation;
import com.fiap.workshop.shared.application.EventPublisher;
import com.fiap.workshop.shared.domain.UuidV7;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class StockReservations {

	private static final String SYSTEM = "system";

	private final PartRepository parts;
	private final StockReservationRepository reservations;
	private final StockMovementRepository movements;
	private final EventPublisher events;
	private final Clock clock;

	StockReservations(PartRepository parts, StockReservationRepository reservations, StockMovementRepository movements,
			EventPublisher events, Clock clock) {
		this.parts = parts;
		this.reservations = reservations;
		this.movements = movements;
		this.events = events;
		this.clock = clock;
	}

	void request(UUID workOrderId, String workOrderNumber, UUID budgetId, List<PartQuantity> requested) {
		if (reservations.findByBudgetId(budgetId).isPresent()) {
			return;
		}
		Instant now = clock.instant();
		StockReservation reservation = reservations.save(StockReservation.request(workOrderId, workOrderNumber, budgetId,
				requested.stream().map(p -> new ReservationLine(p.partId(), p.quantity())).toList(), now));
		List<ShortagePart> shortages = attempt(reservation);
		if (!shortages.isEmpty()) {
			events.publish(new PartsShortage(UuidV7.generate(), now, workOrderId, budgetId, shortages));
		}
	}

	void retryWaiting(Collection<UUID> partIds) {
		for (StockReservation waiting : reservations.findWaitingForParts(partIds)) {
			attempt(waiting);
		}
	}

	void consume(UUID workOrderId) {
		List<StockReservation> held = reservations.findByWorkOrderId(workOrderId).stream().filter(StockReservation::holdsStock).toList();
		if (held.isEmpty()) {
			return;
		}
		Instant now = clock.instant();
		Map<UUID, Part> locked = lock(held);
		for (StockReservation reservation : held) {
			for (ReservationLine line : reservation.lines()) {
				Part part = locked.get(line.partId());
				int availableBefore = part.available();
				var movement = part.consume(line.quantity(), "Work order " + reservation.workOrderNumber(), SYSTEM, now);
				save(part, availableBefore, now);
				movements.save(movement);
			}
			reservation.markConsumed(now);
			reservations.save(reservation);
		}
		events.publish(new ReservationConsumed(UuidV7.generate(), now, workOrderId));
	}

	void release(UUID workOrderId) {
		List<StockReservation> open = reservations.findByWorkOrderId(workOrderId).stream()
				.filter(r -> r.status() == ReservationStatus.WAITING || r.holdsStock()).toList();
		if (open.isEmpty()) {
			return;
		}
		Instant now = clock.instant();
		Map<UUID, Part> locked = lock(open);
		Set<UUID> freed = new HashSet<>();
		for (StockReservation reservation : open) {
			if (reservation.release(now)) {
				for (ReservationLine line : reservation.lines()) {
					Part part = locked.get(line.partId());
					int availableBefore = part.available();
					part.release(line.quantity(), now);
					save(part, availableBefore, now);
					freed.add(line.partId());
				}
			}
			reservations.save(reservation);
		}
		events.publish(new ReservationReleased(UuidV7.generate(), now, workOrderId));
		retryWaiting(freed);
	}

	private List<ShortagePart> attempt(StockReservation reservation) {
		Instant now = clock.instant();
		Map<UUID, Part> locked = lock(List.of(reservation));
		List<ShortagePart> shortages = new ArrayList<>();
		for (ReservationLine line : reservation.lines()) {
			Part part = locked.get(line.partId());
			int available = part == null || !part.active() ? 0 : part.available();
			if (available < line.quantity()) {
				shortages.add(new ShortagePart(line.partId(), line.quantity(), available));
			}
		}
		if (!shortages.isEmpty()) {
			return shortages;
		}
		for (ReservationLine line : reservation.lines()) {
			Part part = locked.get(line.partId());
			int availableBefore = part.available();
			part.reserve(line.quantity(), now);
			save(part, availableBefore, now);
		}
		reservation.markReserved(now);
		reservations.save(reservation);
		events.publish(new PartsReserved(UuidV7.generate(), now, reservation.workOrderId(), reservation.budgetId()));
		return List.of();
	}

	private Map<UUID, Part> lock(Collection<StockReservation> forReservations) {
		Set<UUID> ids = forReservations.stream().flatMap(r -> r.lines().stream()).map(ReservationLine::partId)
				.collect(Collectors.toSet());
		return parts.findAllForUpdate(ids).stream().collect(Collectors.toMap(Part::id, Function.identity()));
	}

	private void save(Part part, int availableBefore, Instant now) {
		parts.save(part);
		if (part.crossedLowStockFrom(availableBefore)) {
			events.publish(InventoryEvents.lowStock(part, now));
		}
	}
}

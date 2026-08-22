package com.fiap.techchallenge.scheduling.controllers;

import com.fiap.techchallenge.scheduling.api.AppointmentService;
import com.fiap.techchallenge.scheduling.api.commands.BookCustomerDropoffCommand;
import com.fiap.techchallenge.scheduling.api.commands.BookDropoffOnBehalfCommand;
import com.fiap.techchallenge.scheduling.api.commands.BookGuestDropoffCommand;
import com.fiap.techchallenge.scheduling.api.commands.BookPickupByTokenCommand;
import com.fiap.techchallenge.scheduling.api.commands.BookPickupCommand;
import com.fiap.techchallenge.scheduling.api.commands.CancelAppointmentCommand;
import com.fiap.techchallenge.scheduling.api.commands.CompleteGuestRegistrationCommand;
import com.fiap.techchallenge.scheduling.api.commands.GuestRescheduleCommand;
import com.fiap.techchallenge.scheduling.api.commands.GuestTokenCommand;
import com.fiap.techchallenge.scheduling.api.commands.RegisterGuestAtCheckInCommand;
import com.fiap.techchallenge.scheduling.api.commands.RescheduleAppointmentCommand;
import com.fiap.techchallenge.scheduling.api.queries.AppointmentFilterQuery;
import com.fiap.techchallenge.scheduling.api.representation.AppointmentInfo;
import com.fiap.techchallenge.scheduling.api.representation.GuestConversionResult;
import com.fiap.techchallenge.scheduling.enums.AppointmentType;
import com.fiap.techchallenge.shared.responses.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Drop-off/pickup scheduling endpoints — staff, customer, and public guest-token flows. Full
 * endpoint documentation lives on {@link AppointmentControllerSwaggerDoc}.
 */
@RestController
@RequiredArgsConstructor
public class AppointmentController implements AppointmentControllerSwaggerDoc {

    private static final String STAFF = "hasAnyRole('ATTENDANT', 'MANAGER')";

    private final AppointmentService service;

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<PageResponse<AppointmentInfo>> getAll(Pageable pageable, AppointmentFilterQuery filter) {
        Page<AppointmentInfo> page = service.list(filter, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> getById(UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @Override
    public ResponseEntity<List<Instant>> availability(AppointmentType type, LocalDate date) {
        return ResponseEntity.ok(service.availableSlots(type, date));
    }

    @Override
    public ResponseEntity<AppointmentInfo> bookGuestDropoff(BookGuestDropoffCommand command) {
        AppointmentInfo appointment = service.bookGuestDropoff(command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentInfo> bookCustomerDropoff(BookCustomerDropoffCommand command, Authentication authentication) {
        AppointmentInfo appointment = service.bookCustomerDropoff(currentUserId(authentication), command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> bookDropoffOnBehalf(BookDropoffOnBehalfCommand command) {
        AppointmentInfo appointment = service.bookDropoffOnBehalf(command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentInfo> bookPickup(BookPickupCommand command, Authentication authentication) {
        AppointmentInfo appointment = service.bookPickup(currentUserId(authentication), command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @Override
    public ResponseEntity<AppointmentInfo> bookPickupByToken(BookPickupByTokenCommand command) {
        AppointmentInfo appointment = service.bookPickupByToken(command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> checkIn(UUID id) {
        return ResponseEntity.ok(service.checkIn(id));
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<GuestConversionResult> registerGuestAtCheckIn(UUID id, RegisterGuestAtCheckInCommand command) {
        return ResponseEntity.ok(service.registerGuestAtCheckIn(id, command));
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> cancel(UUID id, CancelAppointmentCommand command) {
        return ResponseEntity.ok(service.cancel(id, command));
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentInfo> cancelOwn(UUID id, CancelAppointmentCommand command, Authentication authentication) {
        return ResponseEntity.ok(service.cancelOwn(id, currentUserId(authentication), command));
    }

    @Override
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> reschedule(UUID id, RescheduleAppointmentCommand command) {
        return ResponseEntity.ok(service.reschedule(id, command));
    }

    @Override
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentInfo> rescheduleOwn(UUID id, RescheduleAppointmentCommand command, Authentication authentication) {
        return ResponseEntity.ok(service.rescheduleOwn(id, currentUserId(authentication), command));
    }

    @Override
    public ResponseEntity<AppointmentInfo> guestView(GuestTokenCommand command) {
        return ResponseEntity.ok(service.getByToken(command));
    }

    @Override
    public ResponseEntity<AppointmentInfo> guestCancel(GuestTokenCommand command) {
        return ResponseEntity.ok(service.cancelByToken(command));
    }

    @Override
    public ResponseEntity<AppointmentInfo> guestReschedule(GuestRescheduleCommand command) {
        return ResponseEntity.ok(service.rescheduleByToken(command));
    }

    @Override
    public ResponseEntity<GuestConversionResult> completeRegistration(CompleteGuestRegistrationCommand command) {
        return ResponseEntity.ok(service.completeRegistrationViaToken(command));
    }

    private UUID currentUserId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private URI locationOf(UUID id) {
        return ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .path("/appointments/{id}")
                .buildAndExpand(id)
                .toUri();
    }
}

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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private static final String STAFF = "hasAnyRole('ATTENDANT', 'MANAGER')";

    private final AppointmentService service;

    @GetMapping
    @PreAuthorize(STAFF)
    public ResponseEntity<PageResponse<AppointmentInfo>> getAll(
            @PageableDefault Pageable pageable,
            @Valid AppointmentFilterQuery filter
    ) {
        Page<AppointmentInfo> page = service.list(filter, pageable);
        return ResponseEntity.ok(PageResponse.from(page));
    }

    @GetMapping("/{id}")
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @GetMapping("/availability")
    public ResponseEntity<List<Instant>> availability(
            @RequestParam AppointmentType type,
            @RequestParam LocalDate date
    ) {
        return ResponseEntity.ok(service.availableSlots(type, date));
    }

    @PostMapping("/dropoff/guest")
    public ResponseEntity<AppointmentInfo> bookGuestDropoff(@Valid @RequestBody BookGuestDropoffCommand command) {
        AppointmentInfo appointment = service.bookGuestDropoff(command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @PostMapping("/dropoff/customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentInfo> bookCustomerDropoff(
            @Valid @RequestBody BookCustomerDropoffCommand command,
            Authentication authentication
    ) {
        AppointmentInfo appointment = service.bookCustomerDropoff(currentUserId(authentication), command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @PostMapping("/dropoff/on-behalf")
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> bookDropoffOnBehalf(@Valid @RequestBody BookDropoffOnBehalfCommand command) {
        AppointmentInfo appointment = service.bookDropoffOnBehalf(command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @PostMapping("/pickup")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentInfo> bookPickup(
            @Valid @RequestBody BookPickupCommand command,
            Authentication authentication
    ) {
        AppointmentInfo appointment = service.bookPickup(currentUserId(authentication), command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @PostMapping("/pickup/book")
    public ResponseEntity<AppointmentInfo> bookPickupByToken(@Valid @RequestBody BookPickupByTokenCommand command) {
        AppointmentInfo appointment = service.bookPickupByToken(command);
        return ResponseEntity.created(locationOf(appointment.id())).body(appointment);
    }

    @PostMapping("/{id}/check-in")
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> checkIn(@PathVariable UUID id) {
        return ResponseEntity.ok(service.checkIn(id));
    }

    @PostMapping("/{id}/register-guest")
    @PreAuthorize(STAFF)
    public ResponseEntity<GuestConversionResult> registerGuestAtCheckIn(
            @PathVariable UUID id,
            @Valid @RequestBody RegisterGuestAtCheckInCommand command
    ) {
        return ResponseEntity.ok(service.registerGuestAtCheckIn(id, command));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> cancel(
            @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentCommand command
    ) {
        return ResponseEntity.ok(service.cancel(id, command));
    }

    @PostMapping("/{id}/customer-cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentInfo> cancelOwn(
            @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentCommand command,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.cancelOwn(id, currentUserId(authentication), command));
    }

    @PostMapping("/{id}/reschedule")
    @PreAuthorize(STAFF)
    public ResponseEntity<AppointmentInfo> reschedule(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentCommand command
    ) {
        return ResponseEntity.ok(service.reschedule(id, command));
    }

    @PostMapping("/{id}/customer-reschedule")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<AppointmentInfo> rescheduleOwn(
            @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentCommand command,
            Authentication authentication
    ) {
        return ResponseEntity.ok(service.rescheduleOwn(id, currentUserId(authentication), command));
    }

    @PostMapping("/guest/view")
    public ResponseEntity<AppointmentInfo> guestView(@Valid @RequestBody GuestTokenCommand command) {
        return ResponseEntity.ok(service.getByToken(command));
    }

    @PostMapping("/guest/cancel")
    public ResponseEntity<AppointmentInfo> guestCancel(@Valid @RequestBody GuestTokenCommand command) {
        return ResponseEntity.ok(service.cancelByToken(command));
    }

    @PostMapping("/guest/reschedule")
    public ResponseEntity<AppointmentInfo> guestReschedule(@Valid @RequestBody GuestRescheduleCommand command) {
        return ResponseEntity.ok(service.rescheduleByToken(command));
    }

    @PostMapping("/guest/complete-registration")
    public ResponseEntity<GuestConversionResult> completeRegistration(
            @Valid @RequestBody CompleteGuestRegistrationCommand command
    ) {
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

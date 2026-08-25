package com.fiap.techchallenge.scheduling.controllers;

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
import com.fiap.techchallenge.shared.openapi.CommonApiResponses;
import com.fiap.techchallenge.shared.responses.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Full Swagger/OpenAPI contract for {@link AppointmentController}. Mixes staff-facing, customer-facing,
 * and public endpoints — the guest-token-driven ones ({@code guestView}, {@code guestCancel},
 * {@code guestReschedule}, {@code bookPickupByToken}, {@code completeRegistration}) carry no JWT and
 * are public in {@code SecurityConfig}, because possession of the one-time token in the request body
 * is itself the credential (ADR 0015).
 */
@Tag(name = "Appointments", description = "Drop-off/pickup scheduling: staff booking and management, customer self-service, and guest token-based flows.")
@RequestMapping("appointments")
public interface AppointmentControllerSwaggerDoc {

    @Operation(
            summary = "List appointments",
            description = "Returns a paginated, filterable list of appointments. Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @GetMapping
    ResponseEntity<PageResponse<AppointmentInfo>> getAll(
            @PageableDefault Pageable pageable,
            @Valid AppointmentFilterQuery filter
    );

    @Operation(
            summary = "Get an appointment by id",
            description = "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No appointment exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/{id}")
    ResponseEntity<AppointmentInfo> getById(@Parameter(description = "Appointment id") @PathVariable UUID id);

    @Operation(
            summary = "List the caller's own appointments",
            description = "Every appointment belonging to the caller — any status, any type (Drop-off or Pickup), "
                    + "newest first, including appointments originally booked as a Guest and later linked via "
                    + "Guest Conversion. Requires the CUSTOMER role."
    )
    @CommonApiResponses
    @GetMapping("/mine")
    ResponseEntity<PageResponse<AppointmentInfo>> getMine(
            @PageableDefault(sort = "createdAt", direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable,
            @Valid AppointmentFilterQuery filter,
            Authentication authentication
    );

    @Operation(
            summary = "List available slots",
            description = "Returns the open slot start times for the given appointment type and date. Public — no authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The type or date parameter is missing or malformed.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @GetMapping("/availability")
    ResponseEntity<List<Instant>> availability(
            @Parameter(description = "Appointment type (DROPOFF or PICKUP)") @RequestParam AppointmentType type,
            @Parameter(description = "Date to check availability for") @RequestParam LocalDate date
    );

    @Operation(
            summary = "Book a guest drop-off",
            description = "Books a drop-off for a guest with no account, identified by phone/email. Public — no authentication required."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The requested slot is no longer available, or this guest already has a booking pending conversion.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/dropoff/guest")
    ResponseEntity<AppointmentInfo> bookGuestDropoff(@Valid @RequestBody BookGuestDropoffCommand command);

    @Operation(
            summary = "Book a drop-off for the caller's own vehicle",
            description = "Requires the CUSTOMER role. The vehicle must belong to the caller."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "400", description = "The request body failed validation, or the vehicle does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The requested slot is no longer available.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/dropoff/customer")
    ResponseEntity<AppointmentInfo> bookCustomerDropoff(
            @Valid @RequestBody BookCustomerDropoffCommand command,
            Authentication authentication
    );

    @Operation(
            summary = "Book a drop-off on behalf of a customer or guest",
            description = "Staff books a drop-off for an existing customer's vehicle, or for a guest, in one call — exactly "
                    + "one of the two must be supplied. Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "400", description = "The request body failed validation, mixed or omitted the customer/guest fields, or the vehicle does not belong to the given customer.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The requested slot is no longer available.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/dropoff/on-behalf")
    ResponseEntity<AppointmentInfo> bookDropoffOnBehalf(@Valid @RequestBody BookDropoffOnBehalfCommand command);

    @Operation(
            summary = "Book a pickup for the caller's own vehicle",
            description = "Requires the CUSTOMER role. The vehicle must belong to the caller."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "400", description = "The request body failed validation, or the vehicle does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The requested slot is no longer available.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/pickup")
    ResponseEntity<AppointmentInfo> bookPickup(
            @Valid @RequestBody BookPickupCommand command,
            Authentication authentication
    );

    @Operation(
            summary = "Book a pickup via invitation token",
            description = "Books the pickup slot for the customer/vehicle/work order referenced by a one-time pickup "
                    + "invitation token (sent when the vehicle became ready). Public — no authentication required; the "
                    + "token itself is the credential."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "The pickup invitation token is invalid, expired, or already used.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The requested slot is no longer available.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/pickup/book")
    ResponseEntity<AppointmentInfo> bookPickupByToken(@Valid @RequestBody BookPickupByTokenCommand command);

    @Operation(
            summary = "Check in an appointment",
            description = "Marks a SCHEDULED appointment as checked in. Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No appointment exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The appointment is not in a state that allows checking in.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/check-in")
    ResponseEntity<AppointmentInfo> checkIn(@Parameter(description = "Appointment id") @PathVariable UUID id);

    @Operation(
            summary = "Register a walk-in guest at check-in",
            description = "Converts a guest appointment into a full customer/vehicle registration at the check-in desk. "
                    + "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No appointment exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The appointment was already converted to a registered customer.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/register-guest")
    ResponseEntity<GuestConversionResult> registerGuestAtCheckIn(
            @Parameter(description = "Appointment id") @PathVariable UUID id,
            @Valid @RequestBody RegisterGuestAtCheckInCommand command
    );

    @Operation(
            summary = "Cancel an appointment",
            description = "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No appointment exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The appointment is not in a state that allows cancellation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/cancel")
    ResponseEntity<AppointmentInfo> cancel(
            @Parameter(description = "Appointment id") @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentCommand command
    );

    @Operation(
            summary = "Cancel the caller's own appointment",
            description = "Requires the CUSTOMER role. Returns 404 rather than 403 if the appointment belongs to "
                    + "someone else, so as not to reveal whether it exists."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No appointment exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The appointment is not in a state that allows cancellation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/customer-cancel")
    ResponseEntity<AppointmentInfo> cancelOwn(
            @Parameter(description = "Appointment id") @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentCommand command,
            Authentication authentication
    );

    @Operation(
            summary = "Reschedule an appointment",
            description = "Requires the ATTENDANT or MANAGER role."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No appointment exists with the given id.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The appointment is not in a state that allows rescheduling, or the new slot is not available.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/reschedule")
    ResponseEntity<AppointmentInfo> reschedule(
            @Parameter(description = "Appointment id") @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentCommand command
    );

    @Operation(
            summary = "Reschedule the caller's own appointment",
            description = "Requires the CUSTOMER role. Returns 404 rather than 403 if the appointment belongs to "
                    + "someone else, so as not to reveal whether it exists."
    )
    @CommonApiResponses
    @ApiResponse(responseCode = "404", description = "No appointment exists with the given id, or it does not belong to the caller.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The appointment is not in a state that allows rescheduling, or the new slot is not available.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/{id}/customer-reschedule")
    ResponseEntity<AppointmentInfo> rescheduleOwn(
            @Parameter(description = "Appointment id") @PathVariable UUID id,
            @Valid @RequestBody RescheduleAppointmentCommand command,
            Authentication authentication
    );

    @Operation(
            summary = "View an appointment by guest token",
            description = "Public — no authentication required; the token itself is the credential."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "The token is invalid or expired.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/guest/view")
    ResponseEntity<AppointmentInfo> guestView(@Valid @RequestBody GuestTokenCommand command);

    @Operation(
            summary = "Cancel an appointment by guest token",
            description = "Public — no authentication required; the token itself is the credential."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "The token is invalid or expired.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The appointment is not in a state that allows cancellation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/guest/cancel")
    ResponseEntity<AppointmentInfo> guestCancel(@Valid @RequestBody GuestTokenCommand command);

    @Operation(
            summary = "Reschedule an appointment by guest token",
            description = "Public — no authentication required; the token itself is the credential."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "The token is invalid or expired.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "The appointment is not in a state that allows rescheduling, or the new slot is not available.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/guest/reschedule")
    ResponseEntity<AppointmentInfo> guestReschedule(@Valid @RequestBody GuestRescheduleCommand command);

    @Operation(
            summary = "Complete guest registration by token",
            description = "Converts a guest appointment into a full customer/vehicle registration via the registration "
                    + "token sent to the guest. Public — no authentication required; the token itself is the credential."
    )
    @SecurityRequirements
    @ApiResponse(responseCode = "400", description = "The request body failed validation.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "The token is invalid, expired, or already used.",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @PostMapping("/guest/complete-registration")
    ResponseEntity<GuestConversionResult> completeRegistration(@Valid @RequestBody CompleteGuestRegistrationCommand command);
}

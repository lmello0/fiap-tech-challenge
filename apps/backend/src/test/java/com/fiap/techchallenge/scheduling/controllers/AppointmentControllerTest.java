package com.fiap.techchallenge.scheduling.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import com.fiap.techchallenge.user.enums.WorkerRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Staff, customer, and guest-token appointment flows. AvailabilityServiceTest and
 * AppointmentTokenServiceTest already cover slot computation and token issuance/validation at the
 * service layer; this covers the HTTP wiring on top -- role gating, request/response shapes, and the
 * guest-token round trip (a raw token is only ever handed out via the confirmation email, captured
 * here through the same ApplicationEvents seam AuthEmailsTest uses).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class AppointmentControllerTest {

    private static final String PASSWORD = "aVeryLongPassword1";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("appointments/(\\S+)\\?token=(\\S+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    UserAuthRepository userAuths;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ApplicationEvents events;

    final ObjectMapper json = new ObjectMapper();

    @Test
    void anyoneCanCheckAvailabilityWithNoAuthentication() throws Exception {
        LocalDate date = nextWeekday();

        mvc.perform(get("/appointments/availability")
                        .param("type", "DROPOFF")
                        .param("date", date.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void aGuestBooksViewsReschedulesAndCancelsViaToken() throws Exception {
        Instant slot = nextWeekdaySlot(9);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guestDropoffPayload(slot)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.guestName").value("Gil Guest"))
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        String accessToken = tokenFor("manage");

        mvc.perform(post("/appointments/guest/view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenPayload(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()));

        Instant rescheduledSlot = nextWeekdaySlot(11);

        MvcResult rescheduleResult = mvc.perform(post("/appointments/guest/reschedule")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s", "newSlotStart": "%s"}""".formatted(accessToken, rescheduledSlot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotStart").value(rescheduledSlot.toString()))
                .andReturn();
        UUID newAppointmentId = UUID.fromString(json.readTree(rescheduleResult.getResponse().getContentAsString()).get("id").asText());

        // the original token now points at a cancelled, rescheduled-away appointment -- reschedule
        // hands back a new appointment id, not a new token, so the original access token still
        // resolves (to the old, now-cancelled row), matching Reschedule's "cancel and recreate" (ADR 0016)
        mvc.perform(post("/appointments/guest/view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenPayload(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(appointmentId.toString()))
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.rescheduledToId").value(newAppointmentId.toString()));
    }

    @Test
    void aGuestCancelsViaToken() throws Exception {
        Instant slot = nextWeekdaySlot(13);

        mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guestDropoffPayload(slot)))
                .andExpect(status().isCreated());

        String accessToken = tokenFor("manage");

        mvc.perform(post("/appointments/guest/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenPayload(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void anInvalidGuestTokenIsRejected() throws Exception {
        // masked as a plain 404, like an unknown/wrong-owner appointment elsewhere in this module --
        // an invalid token doesn't get a distinguishable 401.
        mvc.perform(post("/appointments/guest/view")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(tokenPayload("not-a-real-token")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aGuestCompletesRegistrationAndBecomesARealCustomer() throws Exception {
        Instant slot = nextWeekdaySlot(14);

        mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guestDropoffPayload(slot)))
                .andExpect(status().isCreated());

        String registrationToken = tokenFor("complete-registration");

        mvc.perform(post("/appointments/guest/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "rawPassword": "%s",
                                  "documentType": "CPF",
                                  "documentCode": "%s",
                                  "licensePlate": "%s",
                                  "vehicleType": "CAR",
                                  "color": "Silver",
                                  "fuelType": "FLEX",
                                  "transmissionType": "MANUAL"
                                }""".formatted(registrationToken, PASSWORD, uniqueDocument(), uniquePlate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").doesNotExist())
                .andExpect(jsonPath("$.appointment.guestEmail").exists());
    }

    @Test
    void attendantBooksOnBehalfOfAGuestAndChecksThemInWithRegistration() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Instant slot = nextWeekdaySlot(15);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/on-behalf")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Walk-in Wanda",
                                  "guestPhone": "%s",
                                  "guestEmail": "%s",
                                  "guestVehicleMake": "Honda",
                                  "guestVehicleModel": "Civic",
                                  "guestVehicleYear": 2021,
                                  "complaint": "Brake noise",
                                  "slotStart": "%s"
                                }""".formatted(uniquePhone(), uniqueEmail(), slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        // registerGuestAtCheckIn only performs Guest Conversion (CONTEXT.md) -- it doesn't itself
        // mark the appointment COMPLETED; that's the separate check-in action right after.
        mvc.perform(post("/appointments/{id}/register-guest", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "documentType": "CPF",
                                  "documentCode": "%s",
                                  "licensePlate": "%s",
                                  "vehicleType": "CAR",
                                  "color": "Red",
                                  "fuelType": "HYBRID",
                                  "transmissionType": "AUTOMATIC"
                                }""".formatted(uniqueDocument(), uniquePlate())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").exists())
                .andExpect(jsonPath("$.appointment.status").value("SCHEDULED"));

        MvcResult checkInResult = mvc.perform(post("/appointments/{id}/check-in", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andReturn();

        assertThat(json.readTree(checkInResult.getResponse().getContentAsString())
                .get("checkedInAt").asText()).isNotBlank();
    }

    @Test
    void aGuestWithNoLastNameCanStillCompleteRegistration() throws Exception {
        Instant slot = nextWeekdaySlot(16, 30);

        mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Cher",
                                  "guestPhone": "%s",
                                  "guestEmail": "%s",
                                  "guestVehicleMake": "Toyota",
                                  "guestVehicleModel": "Corolla",
                                  "guestVehicleYear": 2019,
                                  "complaint": "Won't start",
                                  "slotStart": "%s"
                                }""".formatted(uniquePhone(), uniqueEmail(), slot)))
                .andExpect(status().isCreated());

        String registrationToken = tokenFor("complete-registration");

        mvc.perform(post("/appointments/guest/complete-registration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token": "%s",
                                  "rawPassword": "%s",
                                  "documentType": "CPF",
                                  "documentCode": "%s",
                                  "licensePlate": "%s",
                                  "vehicleType": "CAR",
                                  "color": "Silver",
                                  "fuelType": "FLEX",
                                  "transmissionType": "MANUAL"
                                }""".formatted(registrationToken, PASSWORD, uniqueDocument(), uniquePlate())))
                .andExpect(status().isOk());
    }

    @Test
    void registeringAGuestAtCheckInTwiceIsRejectedTheSecondTime() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Instant slot = nextWeekdaySlot(17, 0);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/on-behalf")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Second Timer",
                                  "guestPhone": "%s",
                                  "guestEmail": "%s",
                                  "guestVehicleMake": "Honda",
                                  "guestVehicleModel": "Civic",
                                  "guestVehicleYear": 2021,
                                  "complaint": "Brake noise",
                                  "slotStart": "%s"
                                }""".formatted(uniquePhone(), uniqueEmail(), slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        String registerGuestPayload = """
                {
                  "documentType": "CPF",
                  "documentCode": "%s",
                  "licensePlate": "%s",
                  "vehicleType": "CAR",
                  "color": "Red",
                  "fuelType": "HYBRID",
                  "transmissionType": "AUTOMATIC"
                }""".formatted(uniqueDocument(), uniquePlate());

        mvc.perform(post("/appointments/{id}/register-guest", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerGuestPayload))
                .andExpect(status().isOk());

        // The appointment is no longer a guest booking after the first conversion, so this fails on
        // that check alone -- the payload's document/plate values don't need to be fresh.
        mvc.perform(post("/appointments/{id}/register-guest", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerGuestPayload))
                .andExpect(status().isConflict());
    }

    @Test
    void aMechanicCannotBookOrCheckInAppointments() throws Exception {
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Instant slot = nextWeekdaySlot(16);

        mvc.perform(post("/appointments/dropoff/on-behalf")
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "X",
                                  "guestPhone": "11900000000",
                                  "guestEmail": "%s",
                                  "guestVehicleMake": "Fiat",
                                  "guestVehicleModel": "Uno",
                                  "guestVehicleYear": 2020,
                                  "complaint": "x",
                                  "slotStart": "%s"
                                }""".formatted(uniqueEmail(), slot)))
                .andExpect(status().isForbidden());

        mvc.perform(get("/appointments")
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void aCustomerBooksAndCancelsTheirOwnDropoff() throws Exception {
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);
        Instant slot = nextWeekdaySlot(10);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/customer")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "%s",
                                  "complaint": "Strange noise",
                                  "slotStart": "%s"
                                }""".formatted(vehicleId, slot)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer.id().toString()))
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/appointments/{id}/customer-cancel", appointmentId)
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Changed my mind"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void aCustomerCanListOnlyTheirOwnAppointments() throws Exception {
        Fixture customer = registerCustomer();
        Fixture other = registerCustomer();
        UUID vehicleId = createVehicle(customer);
        createVehicle(other);
        Instant slot = nextWeekdaySlot(15);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/customer")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "%s",
                                  "complaint": "Strange noise",
                                  "slotStart": "%s"
                                }""".formatted(vehicleId, slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/appointments/mine")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(appointmentId.toString()));

        mvc.perform(get("/appointments/mine")
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    void aCustomerCannotCancelAnotherCustomersAppointment() throws Exception {
        Fixture owner = registerCustomer();
        Fixture other = registerCustomer();
        UUID vehicleId = createVehicle(owner);
        Instant slot = nextWeekdaySlot(9);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/customer")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "%s",
                                  "complaint": "Strange noise",
                                  "slotStart": "%s"
                                }""".formatted(vehicleId, slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/appointments/{id}/customer-cancel", appointmentId)
                        .header("Authorization", "Bearer " + other.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Not mine"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCanListGetCancelAndRescheduleAnyAppointment() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);
        Instant slot = nextWeekdaySlot(9);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/customer")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "%s",
                                  "complaint": "Strange noise",
                                  "slotStart": "%s"
                                }""".formatted(vehicleId, slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(get("/appointments")
                        .param("customerId", customer.id().toString())
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        Instant rescheduledSlot = nextWeekdaySlot(12);
        mvc.perform(post("/appointments/{id}/reschedule", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newSlotStart": "%s"}""".formatted(rescheduledSlot)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slotStart").value(rescheduledSlot.toString()));
    }

    @Test
    void gettingAnUnknownAppointmentReturnsNotFound() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);

        mvc.perform(get("/appointments/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingADropoffOutsideBusinessHoursIsRejected() throws Exception {
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        // 7am is before the seeded 08:00 business start
        Instant tooEarly = nextWeekdaySlot(7);

        mvc.perform(post("/appointments/dropoff/customer")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "%s",
                                  "complaint": "x",
                                  "slotStart": "%s"
                                }""".formatted(vehicleId, tooEarly)))
                .andExpect(status().isConflict());
    }

    @Test
    void bookingOnBehalfWithBothCustomerAndGuestDetailsIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        mvc.perform(post("/appointments/dropoff/on-behalf")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "vehicleId": "%s",
                                  "guestName": "Also A Guest",
                                  "complaint": "x",
                                  "slotStart": "%s"
                                }""".formatted(customer.id(), vehicleId, nextWeekdaySlot(9))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingOnBehalfWithNeitherCustomerNorGuestDetailsIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);

        mvc.perform(post("/appointments/dropoff/on-behalf")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "complaint": "x",
                                  "slotStart": "%s"
                                }""".formatted(nextWeekdaySlot(9))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingOnBehalfForAnExistingCustomerWithoutAVehicleIdIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();

        mvc.perform(post("/appointments/dropoff/on-behalf")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "complaint": "x",
                                  "slotStart": "%s"
                                }""".formatted(customer.id(), nextWeekdaySlot(9))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attendantBooksOnBehalfOfAnExistingCustomer() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        mvc.perform(post("/appointments/dropoff/on-behalf")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "vehicleId": "%s",
                                  "complaint": "Squeaky brakes",
                                  "slotStart": "%s"
                                }""".formatted(customer.id(), vehicleId, nextWeekdaySlot(8, 0))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customer.id().toString()))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()));
    }

    @Test
    void aCustomerBooksAndIsRejectedFromBookingAPickupForAVehicleTheyDontOwn() throws Exception {
        Fixture owner = registerCustomer();
        Fixture other = registerCustomer();
        UUID vehicleId = createVehicle(owner);

        mvc.perform(post("/appointments/pickup")
                        .header("Authorization", "Bearer " + other.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "workOrderId": "%s",
                                  "vehicleId": "%s",
                                  "slotStart": "%s"
                                }""".formatted(UUID.randomUUID(), vehicleId, nextWeekdaySlot(9))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void checkingInAnAlreadyCompletedAppointmentIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Instant slot = nextWeekdaySlot(8, 30);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guestDropoffPayload(slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/appointments/{id}/check-in", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/appointments/{id}/check-in", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void cancellingAnAlreadyCancelledAppointmentIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Instant slot = nextWeekdaySlot(9, 30);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guestDropoffPayload(slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/appointments/{id}/cancel", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "First cancel"}"""))
                .andExpect(status().isOk());

        mvc.perform(post("/appointments/{id}/cancel", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Second cancel"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void reschedulingAnAlreadyCancelledAppointmentIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Instant slot = nextWeekdaySlot(10, 30);

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guestDropoffPayload(slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/appointments/{id}/cancel", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "Cancelled"}"""))
                .andExpect(status().isOk());

        mvc.perform(post("/appointments/{id}/reschedule", appointmentId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newSlotStart": "%s"}""".formatted(nextWeekdaySlot(13))))
                .andExpect(status().isConflict());
    }

    @Test
    void bookingASlotThatIsAlreadyFullyBookedIsRejected() throws Exception {
        // seeded default dropoff capacity is 3 (V33 migration) -- three guest bookings fill the slot.
        // Dedicated slot: no other test in this class books at 11:30, so the count starts clean.
        Instant slot = nextWeekdaySlot(11, 30);

        for (int i = 0; i < 3; i++) {
            mvc.perform(post("/appointments/dropoff/guest")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(guestDropoffPayload(slot)))
                    .andExpect(status().isCreated());
        }

        mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guestDropoffPayload(slot)))
                .andExpect(status().isConflict());
    }

    @Test
    void aGuestCannotHaveTwoActiveDropoffBookingsWithTheSameContactDetails() throws Exception {
        String phone = uniquePhone();
        String email = uniqueEmail();
        // The guest-limit check runs before the availability check, so the second slot's
        // availability is irrelevant -- only the first booking needs a genuinely free slot.
        Instant firstSlot = nextWeekdaySlot(12, 30);
        Instant secondSlot = nextWeekdaySlot(13, 30);

        mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Repeat Offender",
                                  "guestPhone": "%s",
                                  "guestEmail": "%s",
                                  "guestVehicleMake": "Toyota",
                                  "guestVehicleModel": "Corolla",
                                  "guestVehicleYear": 2019,
                                  "complaint": "Won't start",
                                  "slotStart": "%s"
                                }""".formatted(phone, email, firstSlot)))
                .andExpect(status().isCreated());

        mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Repeat Offender",
                                  "guestPhone": "%s",
                                  "guestEmail": "%s",
                                  "guestVehicleMake": "Toyota",
                                  "guestVehicleModel": "Corolla",
                                  "guestVehicleYear": 2019,
                                  "complaint": "Still won't start",
                                  "slotStart": "%s"
                                }""".formatted(phone, email, secondSlot)))
                .andExpect(status().isConflict());
    }

    @Test
    void bookingAGuestDropoffWithARegisteredCustomersEmailIsRejected() throws Exception {
        Fixture customer = registerCustomer();
        Instant slot = nextWeekdaySlot(14, 30);

        mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "guestName": "Impersonator",
                                  "guestPhone": "%s",
                                  "guestEmail": "%s",
                                  "guestVehicleMake": "Toyota",
                                  "guestVehicleModel": "Corolla",
                                  "guestVehicleYear": 2019,
                                  "complaint": "Won't start",
                                  "slotStart": "%s"
                                }""".formatted(uniquePhone(), customer.email(), slot)))
                .andExpect(status().isConflict());
    }

    // --- token extraction -----------------------------------------------------------------------

    private String tokenFor(String pathFragment) {
        List<EmailRequestedEvent> published = events.stream(EmailRequestedEvent.class).toList();

        for (EmailRequestedEvent event : published) {
            Matcher matcher = TOKEN_PATTERN.matcher(event.plainText());
            while (matcher.find()) {
                if (matcher.group(1).equals(pathFragment)) {
                    return matcher.group(2);
                }
            }
        }

        throw new AssertionError("No email link found for appointments/" + pathFragment
                + " among " + published.size() + " published email(s)");
    }

    private String tokenPayload(String token) {
        return """
                {"token": "%s"}""".formatted(token);
    }

    // --- payloads / slot helpers ------------------------------------------------------------------

    private String guestDropoffPayload(Instant slot) {
        return """
                {
                  "guestName": "Gil Guest",
                  "guestPhone": "%s",
                  "guestEmail": "%s",
                  "guestVehicleMake": "Toyota",
                  "guestVehicleModel": "Corolla",
                  "guestVehicleYear": 2019,
                  "complaint": "Won't start",
                  "slotStart": "%s"
                }""".formatted(uniquePhone(), uniqueEmail(), slot);
    }

    /** The next Mon-Fri, at least 3 days out to comfortably clear @Future validation and any TTLs. */
    private static LocalDate nextWeekday() {
        LocalDate date = LocalDate.now().plusDays(3);

        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }

        return date;
    }

    private static Instant nextWeekdaySlot(int hourOfDay) {
        return nextWeekdaySlot(hourOfDay, 0);
    }

    private static Instant nextWeekdaySlot(int hourOfDay, int minute) {
        return ZonedDateTime.of(nextWeekday(), LocalTime.of(hourOfDay, minute), ZoneId.systemDefault()).toInstant();
    }

    // --- auth fixtures (mirrors inventory/controllers/PartControllerTest.java) --------------------

    private Fixture registerCustomer() throws Exception {
        String email = uniqueEmail();

        MvcResult result = mvc.perform(post("/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerPayload(email)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode tokens = json.readTree(result.getResponse().getContentAsString());

        userService.markEmailVerified(idOf(email));

        return new Fixture(idOf(email), email, tokens.get("accessToken").asText());
    }

    private Fixture registerWorker(WorkerRole role) throws Exception {
        String email = uniqueEmail();

        UserInfo user = userService.createWorker(new CreateWorkerCommand(
                userCommand(email),
                role,
                LocalDate.now().minusYears(1),
                LocalDate.now().minusYears(1)
        ));

        setPassword(user.id());
        userService.markEmailVerified(user.id());

        return new Fixture(user.id(), email, login(email).get("accessToken").asText());
    }

    private void setPassword(UUID userId) {
        userAuths.save(UserAuth.local(userId, passwordEncoder.encode(PASSWORD)));
    }

    private JsonNode login(String email) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return json.readTree(result.getResponse().getContentAsString());
    }

    private UUID idOf(String email) {
        return userService.findByEmail(email).orElseThrow().id();
    }

    private String customerPayload(String email) {
        return """
                {
                  "user": %s,
                  "rawPassword": "%s"
                }""".formatted(userJson(email), PASSWORD);
    }

    private String userJson(String email) {
        return """
                {
                  "email": "%s",
                  "firstName": "Ana",
                  "lastName": "Souza",
                  "documentType": "CPF",
                  "documentCode": "%s",
                  "phoneNumbers": [{"type": "MOBILE", "phone": "11999999999", "isPrimary": true}]
                }""".formatted(email, uniqueDocument());
    }

    private CreateUserCommand userCommand(String email) {
        return new CreateUserCommand(
                email,
                "Ana",
                "Souza",
                DocumentType.CPF,
                uniqueDocument(),
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true))
        );
    }

    private UUID createVehicle(Fixture owner) throws Exception {
        MvcResult result = mvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": null,
                                  "vehicleType": "CAR",
                                  "licensePlate": "%s",
                                  "make": "Toyota",
                                  "model": "Corolla",
                                  "color": "Black",
                                  "modelYear": 2022,
                                  "manufactureYear": 2022,
                                  "fuelType": "FLEX",
                                  "transmissionType": "AUTOMATIC"
                                }""".formatted(uniquePlate())))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private static String uniquePlate() {
        return "APT" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    private static String uniqueEmail() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
    }

    private static String uniquePhone() {
        return "119" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
    }

    /** DocumentValidator rejects bad check digits, so the digits have to be computed, not random. */
    private static String uniqueDocument() {
        int[] digits = new int[11];

        for (int i = 0; i < 9; i++) {
            digits[i] = ThreadLocalRandom.current().nextInt(10);
        }

        for (int position = 9; position < 11; position++) {
            int sum = 0;

            for (int i = 0; i < position; i++) {
                sum += digits[i] * (position + 1 - i);
            }

            int remainder = (sum * 10) % 11;
            digits[position] = remainder == 10 ? 0 : remainder;
        }

        StringBuilder cpf = new StringBuilder(11);
        for (int digit : digits) {
            cpf.append(digit);
        }

        return cpf.toString();
    }

    record Fixture(UUID id, String email, String accessToken) {
    }
}

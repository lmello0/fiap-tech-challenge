package com.fiap.techchallenge.scheduling.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Shop-wide scheduling settings and closure days: ATTENDANT and MANAGER can read, only MANAGER can
 * write.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class SchedulingSettingsControllerTest {

    private static final String PASSWORD = "aVeryLongPassword1";

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
    void attendantCanReadButNotWriteSettings() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);

        mvc.perform(get("/scheduling/settings").header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(put("/scheduling/settings")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    void mechanicCannotReachSchedulingSettingsAtAll() throws Exception {
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(get("/scheduling/settings").header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanUpdateSettings() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(put("/scheduling/settings")
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(settingsPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dropoffSlotCapacity").value(3))
                .andExpect(jsonPath("$.pickupSlotCapacity").value(2));
    }

    @Test
    void updatingSettingsWithZeroSlotCapacityIsRejected() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        String payload = """
                {
                  "businessStartTime": "08:00:00",
                  "businessEndTime": "18:00:00",
                  "dropoffSlotCapacity": 0,
                  "pickupSlotCapacity": 2
                }""";

        mvc.perform(put("/scheduling/settings")
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    @Test
    void managerCanCreateListAndDeleteAClosure() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        LocalDate date = LocalDate.now().plusMonths(6);

        mvc.perform(post("/scheduling/closures")
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closurePayload(date, "Staff training day")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(date.toString()));

        mvc.perform(get("/scheduling/closures").header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.date == '%s')]".formatted(date)).exists());

        mvc.perform(delete("/scheduling/closures/{date}", date)
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/scheduling/closures").header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.date == '%s')]".formatted(date)).doesNotExist());
    }

    @Test
    void deletingAClosureThatDoesNotExistIsANoOpNotAnError() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(delete("/scheduling/closures/{date}", LocalDate.now().plusYears(2))
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void aDuplicateClosureDateIsAConflict() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        LocalDate date = LocalDate.now().plusMonths(7);

        mvc.perform(post("/scheduling/closures")
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closurePayload(date, "First")))
                .andExpect(status().isOk());

        mvc.perform(post("/scheduling/closures")
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closurePayload(date, "Second")))
                .andExpect(status().isConflict());
    }

    @Test
    void creatingAClosureInThePastIsRejected() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(post("/scheduling/closures")
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closurePayload(LocalDate.now().minusDays(1), "Yesterday")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void closingADateAutoCancelsItsScheduledAppointmentsAndNotifiesTheGuest() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        // 10 days out (still inside the 30-day booking lookahead) so this can't collide with
        // AppointmentControllerTest's 3-days-out slots -- both classes share the same Testcontainers
        // Postgres instance via Spring context caching.
        LocalDate date = weekdayAtLeast(10);
        Instant slot = ZonedDateTime.of(date, LocalTime.of(9, 0), ZoneId.systemDefault()).toInstant();

        MvcResult bookResult = mvc.perform(post("/appointments/dropoff/guest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(guestDropoffPayload(slot)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID appointmentId = UUID.fromString(json.readTree(bookResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/scheduling/closures")
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(closurePayload(date, "Unplanned shop closure")))
                .andExpect(status().isOk());

        mvc.perform(get("/appointments/{id}", appointmentId)
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.cancelReason").value("MANAGER_CLOSURE"));

        boolean cancellationEmailSent = events.stream(EmailRequestedEvent.class)
                .anyMatch(e -> e.subject().equals("Your appointment was cancelled")
                        && e.plainText().contains("Unplanned shop closure"));
        assertThat(cancellationEmailSent).isTrue();
    }

    // --- payloads -----------------------------------------------------------------------------

    private String settingsPayload() {
        return """
                {
                  "businessStartTime": "08:00:00",
                  "businessEndTime": "18:00:00",
                  "dropoffSlotCapacity": 3,
                  "pickupSlotCapacity": 2
                }""";
    }

    private String guestDropoffPayload(Instant slot) {
        return """
                {
                  "guestName": "Closure Casualty",
                  "guestPhone": "%s",
                  "guestEmail": "%s",
                  "guestVehicleMake": "Toyota",
                  "guestVehicleModel": "Corolla",
                  "guestVehicleYear": 2019,
                  "complaint": "Won't start",
                  "slotStart": "%s"
                }""".formatted(uniquePhone(), uniqueEmail(), slot);
    }

    private static String uniquePhone() {
        return "119" + ThreadLocalRandom.current().nextInt(10000000, 99999999);
    }

    private static LocalDate weekdayAtLeast(int daysOut) {
        LocalDate date = LocalDate.now().plusDays(daysOut);

        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }

        return date;
    }

    private String closurePayload(LocalDate date, String message) {
        return """
                {
                  "date": "%s",
                  "message": "%s"
                }""".formatted(date, message);
    }

    // --- auth fixtures (mirrors inventory/controllers/PartControllerTest.java) --------------------

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

    private static String uniqueEmail() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";
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

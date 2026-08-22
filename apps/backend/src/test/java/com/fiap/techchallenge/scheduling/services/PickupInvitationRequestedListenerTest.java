package com.fiap.techchallenge.scheduling.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.scheduling.api.events.PickupInvitationRequestedEvent;
import com.fiap.techchallenge.shared.audit.EventMetadata;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import com.fiap.techchallenge.vehicle.api.VehicleService;
import com.fiap.techchallenge.vehicle.api.commands.CreateVehicleCommand;
import com.fiap.techchallenge.vehicle.api.representation.VehicleInfo;
import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

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

/**
 * PickupInvitationRequestedEvent (CONTEXT.md "Pickup Appointment"): owned by scheduling, published by
 * workorder when a Work Order reaches WAITING_PICKUP (ADR 0014). Simulated here the same way
 * EmailDeliveryIT simulates workorder-triggered emails -- constructing the event directly, since this
 * module deliberately imports nothing from workorder.
 *
 * <p>The listener's extracted {@link PickupInvitationRequestedListener#handle} is called directly
 * rather than going through {@code ApplicationEventPublisher} + {@code @ApplicationModuleListener}'s
 * real async dispatch: Spring's {@code ApplicationEvents} test support only reliably captures events
 * published on the test's own thread, and calling the {@code @ApplicationModuleListener}-annotated
 * {@code on} method -- even directly -- still goes through the proxy that makes it asynchronous, since
 * interception is keyed to the method, not to how it's invoked (same reasoning as {@code
 * email.schedules.RetryFailedEmails} splitting a plain method out from its {@code @Scheduled} one).
 * {@code handle} carries neither annotation, so calling it keeps this test on one thread while still
 * exercising the real listener logic, token issuance, and email content.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class PickupInvitationRequestedListenerTest {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("appointments/pickup/book\\?token=(\\S+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    VehicleService vehicleService;

    @Autowired
    PickupInvitationRequestedListener listener;

    @Autowired
    ApplicationEvents events;

    final ObjectMapper json = new ObjectMapper();

    @Test
    void aGuestConfirmsAPickupInvitationEmailAndBooksAPickupAppointment() throws Exception {
        UUID customerId = newCustomer();
        UUID vehicleId = newVehicle(customerId);
        UUID workOrderId = UUID.randomUUID();

        listener.handle(new PickupInvitationRequestedEvent(workOrderId, customerId, vehicleId, EventMetadata.system("test", true)));

        String rawToken = extractInvitationToken();

        var bookResult = mvc.perform(MockMvcRequestBuilders.post("/appointments/pickup/book")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s", "slotStart": "%s"}""".formatted(rawToken, nextWeekdaySlot(9))))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andReturn();

        JsonNode body = json.readTree(bookResult.getResponse().getContentAsString());
        assertThat(body.get("customerId").asText()).isEqualTo(customerId.toString());
        assertThat(body.get("vehicleId").asText()).isEqualTo(vehicleId.toString());
        assertThat(body.get("workOrderId").asText()).isEqualTo(workOrderId.toString());
        assertThat(body.get("type").asText()).isEqualTo("PICKUP");
    }

    @Test
    void aPickupInvitationForACustomerThatNoLongerExistsIsLoggedNotThrown() {
        UUID vanishedCustomerId = UUID.randomUUID();
        UUID vehicleId = UUID.randomUUID();
        UUID workOrderId = UUID.randomUUID();

        // Must not throw despite the customer lookup coming back empty -- the listener logs
        // "customer_vanished" and returns instead of sending an email that has nowhere to go.
        listener.handle(new PickupInvitationRequestedEvent(workOrderId, vanishedCustomerId, vehicleId, EventMetadata.system("test", true)));

        boolean pickupEmailSent = events.stream(EmailRequestedEvent.class)
                .anyMatch(e -> e.subject().equals("Your vehicle is ready for pickup"));
        assertThat(pickupEmailSent).isFalse();
    }

    private String extractInvitationToken() {
        for (EmailRequestedEvent event : events.stream(EmailRequestedEvent.class).toList()) {
            Matcher matcher = TOKEN_PATTERN.matcher(event.plainText());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        throw new AssertionError("No pickup invitation email was published");
    }

    private UUID newCustomer() {
        String unique = UUID.randomUUID().toString().replace("-", "");

        UserInfo user = userService.createCustomer(new CreateUserCommand(
                unique.substring(0, 12) + "@example.com",
                "Pat",
                "Pickup",
                DocumentType.CPF,
                uniqueDocument(),
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11988887777", true))
        ));

        return user.id();
    }

    private UUID newVehicle(UUID customerId) {
        VehicleInfo vehicle = vehicleService.create(new CreateVehicleCommand(
                customerId,
                VehicleType.CAR,
                uniquePlate(),
                "Toyota",
                "Corolla",
                "Silver",
                2022,
                2022,
                null,
                FuelType.FLEX,
                TransmissionType.AUTOMATIC
        ));

        return vehicle.id();
    }

    private static String uniquePlate() {
        return "PCK" + ThreadLocalRandom.current().nextInt(1000, 9999);
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

    /** The next Mon-Fri, at least 3 days out to comfortably clear @Future validation and any TTLs. */
    private static LocalDate nextWeekday() {
        LocalDate date = LocalDate.now().plusDays(3);

        while (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }

        return date;
    }

    private static Instant nextWeekdaySlot(int hourOfDay) {
        return ZonedDateTime.of(nextWeekday(), LocalTime.of(hourOfDay, 0), ZoneId.systemDefault()).toInstant();
    }
}

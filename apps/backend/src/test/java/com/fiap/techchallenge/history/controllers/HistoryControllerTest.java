package com.fiap.techchallenge.history.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Generic, staff-only read surface over any aggregate's Timeline. HistoryFlowTest already covers
 * the underlying visibility/query rules by calling HistoryQueryService directly; this covers this
 * controller's own HTTP wiring (role gating, path/query params, 404), which had no coverage at all.
 * Vehicle creation is used as the exercised aggregate since it's the cheapest flow that writes a
 * History entry.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class HistoryControllerTest {

    private static final String PASSWORD = "aVeryLongPassword1";
    private static final String VEHICLE = "VEHICLE";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    UserAuthRepository userAuths;

    @Autowired
    PasswordEncoder passwordEncoder;

    final ObjectMapper json = new ObjectMapper();

    @Test
    void staffCanReadATimelineAndOneOfItsSnapshots() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        MvcResult timelineResult = mvc.perform(get("/history/{type}/{id}", VEHICLE, vehicleId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.content[0].aggregateType").value(VEHICLE))
                .andExpect(jsonPath("$.content[0].aggregateId").value(vehicleId.toString()))
                .andReturn();

        UUID entryId = UUID.fromString(json.readTree(timelineResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText());

        mvc.perform(get("/history/{type}/{id}/entries/{entryId}", VEHICLE, vehicleId, entryId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void aStockistCanAlsoReadHistoryButACustomerCannot() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        mvc.perform(get("/history/{type}/{id}", VEHICLE, vehicleId)
                        .header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(get("/history/{type}/{id}", VEHICLE, vehicleId)
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void anUnknownSnapshotEntryReturnsNotFound() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        mvc.perform(get("/history/{type}/{id}/entries/{entryId}", VEHICLE, vehicleId, UUID.randomUUID())
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anAggregateWithNoHistoryReturnsAnEmptyTimelineNotAnError() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);

        mvc.perform(get("/history/{type}/{id}", VEHICLE, UUID.randomUUID())
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    // --- fixtures -----------------------------------------------------------------------------

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
        return "HST" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    // --- auth fixtures (mirrors vehicle/controllers/VehicleControllerTest.java) --------------------

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

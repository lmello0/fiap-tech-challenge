package com.fiap.techchallenge.vehicle.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Vehicle.customerId is a plain FK to users.users.id, so these tests build customer/staff fixtures
 * and check that a CUSTOMER token can only ever reach its own vehicles while staff can reach and
 * filter across everyone's.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class VehicleControllerTest {

    private static final String PASSWORD = "aVeryLongPassword1";

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
    void aCustomerOnlySeesTheirOwnVehiclesRegardlessOfRequestedCustomerId() throws Exception {
        Fixture owner = registerCustomer();
        Fixture other = registerCustomer();

        createVehicle(owner, null, "AAA1111");
        createVehicle(other, null, "BBB2222");
        createVehicle(other, null, "CCC3333");

        mvc.perform(get("/vehicles").header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("AAA1111"));

        mvc.perform(get("/vehicles")
                        .param("customerId", other.id().toString())
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("AAA1111"));
    }

    @Test
    void aStaffMemberSeesAllVehiclesAndCanFilterByAnyCustomerId() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture a = registerCustomer();
        Fixture b = registerCustomer();

        createVehicle(a, null, "AAA1112");
        createVehicle(b, null, "BBB2223");

        mvc.perform(get("/vehicles")
                        .param("customerId", a.id().toString())
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("AAA1112"));
    }

    @Test
    void aUserHoldingBothCustomerAndManagerFacetsIsTreatedAsStaffNotSelfRestricted() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture other = registerCustomer();

        createVehicle(manager, manager.id(), "MGR0001");
        createVehicle(other, null, "OTH0002");

        mvc.perform(get("/vehicles")
                        .param("customerId", other.id().toString())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].licensePlate").value("OTH0002"));
    }

    @Test
    void aCustomerCanCreateOnlyTheirOwnVehicleEvenIfAnotherCustomerIdIsSupplied() throws Exception {
        Fixture owner = registerCustomer();
        Fixture other = registerCustomer();

        MvcResult result = mvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclePayload(other.id(), "SPF0001")))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = json.readTree(result.getResponse().getContentAsString());
        assertThat(created.get("customerId").asText()).isEqualTo(owner.id().toString());
    }

    @Test
    void aStaffMemberMustSupplyCustomerIdWhenCreating() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);

        mvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclePayload(null, "NOCUST1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aCustomerCannotUpdateOrDeleteAnotherCustomersVehicle() throws Exception {
        Fixture owner = registerCustomer();
        Fixture other = registerCustomer();

        UUID vehicleId = createVehicle(owner, null, "LCK0001");

        // 404, not 403 -- ownership mismatches don't reveal that the vehicle exists, same masking
        // as the workorder and appointment modules.
        mvc.perform(patch("/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + other.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("LCK0002")))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aCustomerCanUpdateAndDeleteTheirOwnVehicle() throws Exception {
        Fixture owner = registerCustomer();
        UUID vehicleId = createVehicle(owner, null, "OWN0001");

        mvc.perform(patch("/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("OWN0002")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.licensePlate").value("OWN0002"));

        mvc.perform(delete("/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void staffCanUpdateAndDeleteAnyVehicle() throws Exception {
        Fixture owner = registerCustomer();
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        UUID vehicleId = createVehicle(owner, null, "STF0001");

        mvc.perform(patch("/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("STF0002")))
                .andExpect(status().isOk());

        mvc.perform(delete("/vehicles/{id}", vehicleId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void duplicateLicensePlateIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture a = registerCustomer();
        Fixture b = registerCustomer();

        createVehicle(attendant, a.id(), "DUP0001");

        mvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclePayload(b.id(), "DUP0001")))
                .andExpect(status().isConflict());
    }

    @Test
    void gettingOrMutatingAnUnknownVehicleReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        UUID randomId = UUID.randomUUID();

        mvc.perform(get("/vehicles/{id}", randomId)
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/vehicles/{id}", randomId)
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("XXX9999")))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/vehicles/{id}", randomId)
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
    }

    // --- vehicle fixtures -------------------------------------------------------------------------

    private UUID createVehicle(Fixture caller, UUID customerId, String licensePlate) throws Exception {
        MvcResult result = mvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + caller.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclePayload(customerId, licensePlate)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String vehiclePayload(UUID customerId, String licensePlate) {
        return """
                {
                  "customerId": %s,
                  "vehicleType": "CAR",
                  "licensePlate": "%s",
                  "make": "Toyota",
                  "model": "Corolla",
                  "color": "Black",
                  "modelYear": 2022,
                  "manufactureYear": 2022,
                  "fuelType": "FLEX",
                  "transmissionType": "AUTOMATIC"
                }""".formatted(customerId == null ? "null" : "\"" + customerId + "\"", licensePlate);
    }

    private String updatePayload(String licensePlate) {
        return """
                {
                  "vehicleType": "CAR",
                  "licensePlate": "%s",
                  "make": "Toyota",
                  "model": "Corolla",
                  "color": "Blue",
                  "modelYear": 2023,
                  "manufactureYear": 2023,
                  "fuelType": "FLEX",
                  "transmissionType": "MANUAL"
                }""".formatted(licensePlate);
    }

    // --- auth fixtures (mirrors auth/AuthorizationTest.java) --------------------------------------

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

package com.fiap.techchallenge.inventory.controllers;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Mirrors PartControllerTest's role shape: MECHANIC is read-only, STOCKIST and MANAGER hold full
 * write access, no other role reaches the catalog at all.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class RepairServiceControllerTest {

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
    void mechanicCanReadButNotWriteServices() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        UUID serviceId = createService(stockist, "MEC-READ-1");

        mvc.perform(get("/services").header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(get("/services/{id}", serviceId).header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/services")
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicePayload("MEC-WRITE-1")))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/services/{id}", serviceId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload()))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/services/{id}", serviceId).header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReachServicesAtAll() throws Exception {
        Fixture customer = registerCustomer();

        mvc.perform(get("/services").header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void stockistAndManagerCanCreateUpdateAndDeactivateServices() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        UUID stockistService = createService(stockist, "STK-001");
        UUID managerService = createService(manager, "MGR-001");

        mvc.perform(patch("/services/{id}", stockistService)
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Brake Job"));

        mvc.perform(delete("/services/{id}", managerService).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/services/{id}", managerService).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void duplicateCodeIsRejected() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        createService(stockist, "DUP-001");

        mvc.perform(post("/services")
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicePayload("DUP-001")))
                .andExpect(status().isConflict());
    }

    @Test
    void gettingOrMutatingAnUnknownServiceReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        UUID randomId = UUID.randomUUID();

        mvc.perform(get("/services/{id}", randomId).header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/services/{id}", randomId).header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingWithANonPositiveEstimatedSecondsIsRejected() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);

        String payload = """
                {
                  "code": "BAD-EST-1",
                  "name": "Bad Estimate",
                  "description": "x",
                  "price": 100.00,
                  "estimatedSeconds": 0
                }""";

        mvc.perform(post("/services")
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // --- service fixtures -----------------------------------------------------------------------

    private UUID createService(Fixture caller, String code) throws Exception {
        MvcResult result = mvc.perform(post("/services")
                        .header("Authorization", "Bearer " + caller.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(servicePayload(code)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String servicePayload(String code) {
        return """
                {
                  "code": "%s",
                  "name": "Brake Job",
                  "description": "Front axle brake pad replacement",
                  "price": 250.00,
                  "estimatedSeconds": 1800
                }""".formatted(code);
    }

    private String updatePayload() {
        return """
                {
                  "name": "Updated Brake Job",
                  "description": "Front axle brake pad replacement",
                  "price": 275.00,
                  "estimatedSeconds": 1800
                }""";
    }

    // --- auth fixtures (mirrors PartControllerTest.java) -----------------------------------------

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

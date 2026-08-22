package com.fiap.techchallenge.inventory.controllers;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers the part catalog and stock adjustment surface: MECHANIC is read-only, STOCKIST and MANAGER
 * hold full write access (see the "STOCKIST owns all of inventory" decision — MANAGER's rights are a
 * superset, not a separate carve-out), and no other role reaches inventory at all.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class PartControllerTest {

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
    void mechanicCanReadButNotWriteParts() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        UUID partId = createPart(stockist, "MEC-READ-1");

        mvc.perform(get("/parts").header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(get("/parts/{id}", partId).header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/parts")
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partPayload("MEC-WRITE-1")))
                .andExpect(status().isForbidden());

        mvc.perform(patch("/parts/{id}", partId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload()))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/parts/{id}", partId).header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/parts/{id}/stock/adjustments", partId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustmentPayload("10", "Initial count")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotReachInventoryAtAll() throws Exception {
        Fixture customer = registerCustomer();

        mvc.perform(get("/parts").header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void stockistAndManagerCanCreateUpdateAndDeactivateParts() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        UUID stockistPart = createPart(stockist, "STK-001");
        UUID managerPart = createPart(manager, "MGR-001");

        mvc.perform(patch("/parts/{id}", stockistPart)
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Brake Pad"));

        mvc.perform(delete("/parts/{id}", managerPart).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/parts/{id}", managerPart).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void duplicateSkuIsRejected() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        createPart(stockist, "DUP-001");

        mvc.perform(post("/parts")
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partPayload("DUP-001")))
                .andExpect(status().isConflict());
    }

    @Test
    void gettingOrMutatingAnUnknownPartReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        UUID randomId = UUID.randomUUID();

        mvc.perform(get("/parts/{id}", randomId).header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/parts/{id}", randomId).header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void adjustingStockUpdatesQuantityAndRecordsAMovement() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        UUID partId = createPart(stockist, "ADJ-001");

        mvc.perform(post("/parts/{id}/stock/adjustments", partId)
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustmentPayload("10", "Initial physical count")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(10))
                .andExpect(jsonPath("$.available").value(10));

        mvc.perform(post("/parts/{id}/stock/adjustments", partId)
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustmentPayload("-3", "Breakage")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityOnHand").value(7));

        mvc.perform(get("/parts/{id}/stock/movements", partId)
                        .header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].type").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.content[0].reason").value("Breakage"));
    }

    @Test
    void adjustmentThatWouldDropOnHandBelowZeroIsRejected() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        UUID partId = createPart(stockist, "ADJ-NEG-1");

        mvc.perform(post("/parts/{id}/stock/adjustments", partId)
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustmentPayload("-1", "Impossible")))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- part fixtures ------------------------------------------------------------------------

    private UUID createPart(Fixture caller, String sku) throws Exception {
        MvcResult result = mvc.perform(post("/parts")
                        .header("Authorization", "Bearer " + caller.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(partPayload(sku)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String partPayload(String sku) {
        return """
                {
                  "sku": "%s",
                  "name": "Brake Pad",
                  "description": "Front axle brake pad set",
                  "brand": "Bosch",
                  "unitOfMeasure": "SET",
                  "salePrice": 180.00
                }""".formatted(sku);
    }

    private String updatePayload() {
        return """
                {
                  "name": "Updated Brake Pad",
                  "description": "Front axle brake pad set",
                  "brand": "Bosch",
                  "unitOfMeasure": "SET",
                  "salePrice": 199.90
                }""";
    }

    private String adjustmentPayload(String quantity, String reason) {
        return """
                {
                  "quantity": %s,
                  "reason": "%s"
                }""".formatted(quantity, reason);
    }

    // --- auth fixtures (mirrors vehicle/controllers/VehicleControllerTest.java) ---------------

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

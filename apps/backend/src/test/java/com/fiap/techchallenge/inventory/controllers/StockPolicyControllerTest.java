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
 * Per-part, per-vendor automatic-reorder thresholds. StockPolicyFlowTest already covers the
 * position/evaluation arithmetic at the service layer; this covers the HTTP wiring on top, which had
 * no coverage at all.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class StockPolicyControllerTest {

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
    void mechanicCanReadButNotWriteStockPolicys() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID vendorId = createVendor(stockist);
        UUID partId = createPart(stockist, "RR-MEC-1");

        UUID ruleId = createRule(stockist, partId, vendorId);

        mvc.perform(get("/stock-policies").header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/stock-policies/{id}", ruleId).header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());

        mvc.perform(post("/stock-policies")
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rulePayload(partId, vendorId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void stockistCanCreateListGetUpdateAndDeleteARule() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        UUID vendorId = createVendor(stockist);
        UUID otherVendorId = createVendor(stockist);
        UUID partId = createPart(stockist, "RR-001");

        UUID ruleId = createRule(stockist, partId, vendorId);

        mvc.perform(get("/stock-policies/{id}", ruleId).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minQuantity").value(5))
                .andExpect(jsonPath("$.autoReorderEnabled").value(true));

        mvc.perform(get("/stock-policies")
                        .param("partId", partId.toString())
                        .header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mvc.perform(patch("/stock-policies/{id}", ruleId)
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minQuantity": 8, "maxQuantity": 30, "vendorId": "%s", "autoReorderEnabled": false}""".formatted(otherVendorId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.minQuantity").value(8))
                .andExpect(jsonPath("$.autoReorderEnabled").value(false));

        mvc.perform(delete("/stock-policies/{id}", ruleId).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/stock-policies/{id}", ruleId).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void gettingUpdatingOrDeletingAnUnknownRuleReturnsNotFound() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        UUID vendorId = createVendor(stockist);
        UUID randomId = UUID.randomUUID();

        mvc.perform(get("/stock-policies/{id}", randomId).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isNotFound());

        mvc.perform(patch("/stock-policies/{id}", randomId)
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"minQuantity": 1, "maxQuantity": 2, "vendorId": "%s", "autoReorderEnabled": true}""".formatted(vendorId)))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/stock-policies/{id}", randomId).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingWithMaxNotExceedingMinIsRejected() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        UUID vendorId = createVendor(stockist);
        UUID partId = createPart(stockist, "RR-BAD-1");

        mvc.perform(post("/stock-policies")
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"partId": "%s", "minQuantity": 10, "maxQuantity": 10, "vendorId": "%s", "autoReorderEnabled": true}"""
                                .formatted(partId, vendorId)))
                .andExpect(status().isConflict());
    }

    // --- fixtures ---------------------------------------------------------------------------------

    private UUID createRule(Fixture caller, UUID partId, UUID vendorId) throws Exception {
        MvcResult result = mvc.perform(post("/stock-policies")
                        .header("Authorization", "Bearer " + caller.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(rulePayload(partId, vendorId)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String rulePayload(UUID partId, UUID vendorId) {
        return """
                {"partId": "%s", "minQuantity": 5, "maxQuantity": 20, "vendorId": "%s", "autoReorderEnabled": true}"""
                .formatted(partId, vendorId);
    }

    private UUID createVendor(Fixture caller) throws Exception {
        MvcResult result = mvc.perform(post("/vendors")
                        .header("Authorization", "Bearer " + caller.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "RR Vendor %s", "contactEmail": "vendor@example.com"}""".formatted(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createPart(Fixture caller, String sku) throws Exception {
        MvcResult result = mvc.perform(post("/parts")
                        .header("Authorization", "Bearer " + caller.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "%s",
                                  "name": "Brake Pad",
                                  "description": "Front axle brake pad set",
                                  "brand": "Bosch",
                                  "unitOfMeasure": "SET",
                                  "salePrice": 180.00
                                }""".formatted(sku)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    // --- auth fixtures (mirrors PartControllerTest.java) -------------------------------------------

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

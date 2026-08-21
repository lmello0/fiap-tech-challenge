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
 * STOCKIST and MANAGER hold full access to vendors; no other role reaches this resource, and there
 * is no MECHANIC read-only carve-out here the way there is for parts/services.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class VendorControllerTest {

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
    void mechanicCannotReachVendorsAtAll() throws Exception {
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(get("/vendors").header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void stockistAndManagerCanCreateReadUpdateAndDeactivateVendors() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        UUID vendorId = createVendor(stockist, "Acme Parts Co");

        mvc.perform(get("/vendors/{id}", vendorId).header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Parts Co"));

        mvc.perform(patch("/vendors/{id}", vendorId)
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acme Parts Company"));

        mvc.perform(delete("/vendors/{id}", vendorId).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isNoContent());

        mvc.perform(get("/vendors/{id}", vendorId).header("Authorization", "Bearer " + stockist.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void gettingOrMutatingAnUnknownVendorReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        UUID randomId = UUID.randomUUID();

        mvc.perform(get("/vendors/{id}", randomId).header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());

        mvc.perform(delete("/vendors/{id}", randomId).header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingWithAnInvalidContactEmailIsRejected() throws Exception {
        Fixture stockist = registerWorker(WorkerRole.STOCKIST);

        String payload = """
                {
                  "name": "Bad Email Vendor",
                  "contactEmail": "not-an-email"
                }""";

        mvc.perform(post("/vendors")
                        .header("Authorization", "Bearer " + stockist.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }

    // --- vendor fixtures --------------------------------------------------------------------------

    private UUID createVendor(Fixture caller, String name) throws Exception {
        MvcResult result = mvc.perform(post("/vendors")
                        .header("Authorization", "Bearer " + caller.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vendorPayload(name)))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private String vendorPayload(String name) {
        return """
                {
                  "name": "%s",
                  "contactEmail": "sales@example.com"
                }""".formatted(name);
    }

    private String updatePayload() {
        return """
                {
                  "name": "Acme Parts Company",
                  "contactEmail": "sales@example.com"
                }""";
    }

    // --- auth fixtures (mirrors PartControllerTest.java) -----------------------------------------

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

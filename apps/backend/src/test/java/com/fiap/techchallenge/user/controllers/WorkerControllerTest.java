package com.fiap.techchallenge.user.controllers;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MANAGER-only worker management, plus a worker's self-service profile update. A MANAGER may
 * also update any worker's profile, mirroring the staff override on CustomerController.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class WorkerControllerTest {

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
    void managerCanListWorkers() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(get("/workers")
                        .param("email", mechanic.email())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(mechanic.id().toString()));
    }

    @Test
    void aMechanicCannotListWorkers() throws Exception {
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(get("/workers")
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanGetAWorkerById() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(get("/workers/{id}", mechanic.id())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(mechanic.email()));
    }

    @Test
    void gettingAnUnknownWorkerReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(get("/workers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aWorkerCanUpdateTheirOwnProfile() throws Exception {
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(patch("/workers/{id}", mechanic.id())
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Nova", "8811")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nova"));
    }

    @Test
    void aManagerCanUpdateAnotherWorkersProfile() throws Exception {
        // mirrors the staff override on CustomerController
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(patch("/workers/{id}", mechanic.id())
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Nova", "8822")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nova"));
    }

    @Test
    void aManagerUpdatingAnUnknownWorkerGetsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(patch("/workers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Nova", "8833")))
                .andExpect(status().isNotFound());
    }

    @Test
    void aMechanicCannotUpdateAnotherWorkersProfile() throws Exception {
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture otherMechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(patch("/workers/{id}", otherMechanic.id())
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Nova", "8844")))
                .andExpect(status().isForbidden());
    }

    @Test
    void managerCanTerminateAWorker() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(delete("/workers/{id}", mechanic.id())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(userService.getById(mechanic.id()).terminationDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void aMechanicCannotTerminateAWorker() throws Exception {
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture other = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(delete("/workers/{id}", other.id())
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void terminatingAnUnknownWorkerReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(delete("/workers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void terminatingAUserWithNoWorkerFacetIsUnprocessable() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture customerOnly = registerCustomer();

        mvc.perform(delete("/workers/{id}", customerOnly.id())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- payloads ---------------------------------------------------------------------------------

    private String updatePayload(String firstName, String phoneSuffix) {
        return """
                {
                  "firstName": "%s",
                  "lastName": "Souza",
                  "phoneNumbers": [{"type": "MOBILE", "phone": "1199999%s", "isPrimary": true}]
                }""".formatted(firstName, phoneSuffix);
    }

    // --- auth fixtures (mirrors auth/AuthorizationTest.java) --------------------------------------

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

    private Fixture registerCustomer() throws Exception {
        String email = uniqueEmail();

        UserInfo user = userService.createCustomer(userCommand(email));

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

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
 * Staff management of Customer accounts, plus a Customer's self-service profile update/deactivate.
 * Mirrors the auth/customer fixture pattern used across the other controller test slices.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class CustomerControllerTest {

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
    void staffCanListCustomers() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();

        mvc.perform(get("/customers")
                        .param("email", customer.email())
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(customer.id().toString()));
    }

    @Test
    void aCustomerCannotListCustomers() throws Exception {
        Fixture customer = registerCustomer();

        mvc.perform(get("/customers")
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void staffCanGetACustomerById() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture customer = registerCustomer();

        mvc.perform(get("/customers/{id}", customer.id())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(customer.email()));
    }

    @Test
    void gettingAnUnknownCustomerReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(get("/customers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aCustomerCannotGetAnotherCustomerById() throws Exception {
        Fixture customer = registerCustomer();
        Fixture other = registerCustomer();

        // getById, unlike update/deactivate, has no self-access carve-out -- staff only.
        mvc.perform(get("/customers/{id}", other.id())
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void aCustomerCanUpdateTheirOwnProfile() throws Exception {
        Fixture customer = registerCustomer();

        mvc.perform(patch("/customers/{id}", customer.id())
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Nova", "8888")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nova"));
    }

    @Test
    void staffCanUpdateAnyCustomersProfile() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();

        mvc.perform(patch("/customers/{id}", customer.id())
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Nova", "7777")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Nova"));
    }

    @Test
    void aCustomerCannotUpdateAnotherCustomersProfile() throws Exception {
        Fixture customer = registerCustomer();
        Fixture other = registerCustomer();

        mvc.perform(patch("/customers/{id}", other.id())
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Nova", "6666")))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatingAnUnknownCustomerReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(patch("/customers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("Nova", "5555")))
                .andExpect(status().isNotFound());
    }

    @Test
    void updatingWithABlankFirstNameIsRejected() throws Exception {
        Fixture customer = registerCustomer();

        mvc.perform(patch("/customers/{id}", customer.id())
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload("", "4444")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatingWithTwoPrimaryPhoneNumbersIsAConflict() throws Exception {
        Fixture customer = registerCustomer();

        String payload = """
                {
                  "firstName": "Nova",
                  "lastName": "Souza",
                  "phoneNumbers": [
                    {"type": "MOBILE", "phone": "11988880001", "isPrimary": true},
                    {"type": "HOME", "phone": "1133330002", "isPrimary": true}
                  ]
                }""";

        mvc.perform(patch("/customers/{id}", customer.id())
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict());
    }

    @Test
    void aCustomerCanDeactivateTheirOwnAccount() throws Exception {
        Fixture customer = registerCustomer();

        mvc.perform(delete("/customers/{id}", customer.id())
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(userService.getById(customer.id()).customerActive()).isFalse();
    }

    @Test
    void staffCanDeactivateAnyCustomer() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture customer = registerCustomer();

        mvc.perform(delete("/customers/{id}", customer.id())
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isNoContent());
    }

    @Test
    void aCustomerCannotDeactivateAnotherCustomersAccount() throws Exception {
        Fixture customer = registerCustomer();
        Fixture other = registerCustomer();

        mvc.perform(delete("/customers/{id}", other.id())
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivatingAnUnknownCustomerReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(delete("/customers/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void staffCanReactivateADeactivatedCustomer() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture customer = registerCustomer();
        userService.deactivateCustomer(customer.id());

        mvc.perform(post("/customers/{id}/reactivate", customer.id())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNoContent());

        assertThat(userService.getById(customer.id()).customerActive()).isTrue();
    }

    @Test
    void aCustomerCannotReactivateAnAccount() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture customer = registerCustomer();
        userService.deactivateCustomer(customer.id());

        mvc.perform(post("/customers/{id}/reactivate", customer.id())
                        .header("Authorization", "Bearer " + manager.accessToken()));

        Fixture other = registerCustomer();

        mvc.perform(post("/customers/{id}/reactivate", other.id())
                        .header("Authorization", "Bearer " + other.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void reactivatingAnUnknownCustomerReturnsNotFound() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        mvc.perform(post("/customers/{id}/reactivate", UUID.randomUUID())
                        .header("Authorization", "Bearer " + manager.accessToken()))
                .andExpect(status().isNotFound());
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

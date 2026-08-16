package com.fiap.techchallenge.auth;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Route guards are driven entirely by the facets a user holds, so these tests build users with
 * different facet combinations and check what each token can reach.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthorizationTest {

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
    void aCustomerMayNotReachAWorkerOnlyRoute() throws Exception {
        Fixture customer = registerCustomer();
        Fixture target = registerCustomer();

        mvc.perform(get("/users/{id}", target.id())
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void aWorkerMayReachAWorkerOnlyRoute() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);
        Fixture worker = registerWorkerVia(manager, WorkerRole.MECHANIC);
        Fixture target = registerCustomer();

        mvc.perform(get("/users/{id}", target.id())
                        .header("Authorization", "Bearer " + worker.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void onlyAManagerMayOnboardAWorker() throws Exception {
        Fixture customer = registerCustomer();

        mvc.perform(post("/auth/register/worker")
                        .header("Authorization", "Bearer " + customer.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workerPayload(WorkerRole.MECHANIC)))
                .andExpect(status().isForbidden());

        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);

        mvc.perform(post("/auth/register/worker")
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workerPayload(WorkerRole.MECHANIC)))
                .andExpect(status().isForbidden());
    }

    @Test
    void aUserHoldingBothFacetsCarriesBothAuthorities() throws Exception {
        Fixture manager = registerWorker(WorkerRole.MANAGER);

        // give the manager the customer facet too — the shop's staff can be its clients
        UserInfo both = userService.getById(manager.id());
        assertThat(both.customer()).isTrue();
        assertThat(both.worker()).isTrue();

        // a fresh login picks up the added facet
        JsonNode session = login(manager.email());

        Fixture target = registerCustomer();

        // reaches the worker-only route
        mvc.perform(get("/users/{id}", target.id())
                        .header("Authorization", "Bearer " + session.get("accessToken").asText()))
                .andExpect(status().isOk());

        // and still reaches its own customer view
        mvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + session.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    /**
     * Every worker also carries an active Customer facet (see UserServiceImpl#createWorker), so
     * terminating the Worker facet does not lock the account out entirely — ADR 0001. It does drop
     * the WORKER authority, so worker-only routes stop being reachable.
     */
    @Test
    void aTerminatedWorkerKeepsCustomerAccessButLosesWorkerAuthority() throws Exception {
        Fixture worker = registerWorker(WorkerRole.MECHANIC);
        Fixture target = registerCustomer();

        userService.terminateWorker(worker.id(), LocalDate.now());

        JsonNode session = login(worker.email());

        mvc.perform(get("/users/{id}", target.id())
                        .header("Authorization", "Bearer " + session.get("accessToken").asText()))
                .andExpect(status().isForbidden());

        mvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + session.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    // --- fixtures -------------------------------------------------------------------------------

    private Fixture registerCustomer() throws Exception {
        String email = uniqueEmail();

        MvcResult result = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customerPayload(email)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode tokens = json.readTree(result.getResponse().getContentAsString());

        userService.markEmailVerified(idOf(email));

        return new Fixture(idOf(email), email, tokens.get("accessToken").asText());
    }

    /**
     * Bootstraps a worker directly through the service, since onboarding over HTTP needs a manager
     * token that does not exist yet on a fresh database.
     */
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

    private Fixture registerWorkerVia(Fixture manager, WorkerRole role) throws Exception {
        MvcResult result = mvc.perform(post("/auth/register/worker")
                        .header("Authorization", "Bearer " + manager.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(workerPayload(role)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode created = json.readTree(result.getResponse().getContentAsString());
        String createdEmail = created.get("email").asText();

        userService.markEmailVerified(UUID.fromString(created.get("id").asText()));

        return new Fixture(
                UUID.fromString(created.get("id").asText()),
                createdEmail,
                login(createdEmail).get("accessToken").asText());
    }

    /**
     * Workers bootstrapped through the service have no credential yet; give them the shared test
     * password so they can sign in over HTTP.
     */
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

    private String workerPayload(WorkerRole role) {
        return """
                {
                  "worker": {
                    "user": %s,
                    "registration": "REG-%s",
                    "role": "%s",
                    "hireDate": "%s",
                    "startDate": "%s"
                  },
                  "rawPassword": "%s"
                }""".formatted(
                userJson(uniqueEmail()),
                UUID.randomUUID().toString().substring(0, 8),
                role,
                LocalDate.now().minusYears(1),
                LocalDate.now().minusYears(1),
                PASSWORD);
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

    private static String uniqueDocument() {
        return UUID.randomUUID().toString().replaceAll("\\D", "0").substring(0, 11);
    }

    record Fixture(UUID id, String email, String accessToken) {
    }
}

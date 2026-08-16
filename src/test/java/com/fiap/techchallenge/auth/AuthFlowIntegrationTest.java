package com.fiap.techchallenge.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.repositories.RefreshTokenRepository;
import com.fiap.techchallenge.user.api.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class AuthFlowIntegrationTest {

    private static final String PASSWORD = "aVeryLongPassword1";

    @Autowired
    MockMvc mvc;

    final ObjectMapper json = new ObjectMapper();

    @Autowired
    RefreshTokenRepository refreshTokens;

    @Autowired
    UserService userService;

    private String email;
    private String document;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().replace("-", "");

        this.email = unique.substring(0, 12) + "@example.com";
        this.document = unique.replaceAll("\\D", "0").substring(0, 11);
    }

    @Test
    void registersLogsInAndReachesAProtectedRoute() throws Exception {
        JsonNode registration = register();

        assertThat(registration.get("accessToken").asText()).isNotBlank();
        assertThat(registration.get("refreshToken").asText()).isNotBlank();

        JsonNode login = login();

        mvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + login.get("accessToken").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.customer").value(true))
                .andExpect(jsonPath("$.worker").value(false));
    }

    @Test
    void rejectsAnUnauthenticatedRequest() throws Exception {
        mvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAWrongPasswordWithoutRevealingTheAccount() throws Exception {
        register();

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"email": "%s", "rawPassword": "totallyWrongPassword1"}""".formatted(email))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"email": "nobody@example.com", "rawPassword": "totallyWrongPassword1"}""")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void refreshRotatesTheTokenAndInvalidatesTheOldOne() throws Exception {
        String original = register().get("refreshToken").asText();

        JsonNode refreshed = refresh(original);
        String rotated = refreshed.get("refreshToken").asText();

        assertThat(rotated).isNotEqualTo(original);
        assertThat(refreshed.get("accessToken").asText()).isNotBlank();

        // the rotated token still works
        mvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + refreshed.get("accessToken").asText()))
                .andExpect(status().isOk());
    }

    @Test
    void replayingARotatedRefreshTokenRevokesEverySession() throws Exception {
        JsonNode registration = register();
        String original = registration.get("refreshToken").asText();

        String rotated = refresh(original).get("refreshToken").asText();

        // replay the token that was already consumed
        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"refreshToken": "%s"}""".formatted(original))))
                .andExpect(status().isUnauthorized());

        // the reuse must have taken the whole session family down, not just the replayed token
        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"refreshToken": "%s"}""".formatted(rotated))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutEndsOnlyTheSessionItWasGiven() throws Exception {
        String sessionOne = register().get("refreshToken").asText();
        String sessionTwo = login().get("refreshToken").asText();

        mvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"refreshToken": "%s"}""".formatted(sessionOne))))
                .andExpect(status().isNoContent());

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"refreshToken": "%s"}""".formatted(sessionOne))))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"refreshToken": "%s"}""".formatted(sessionTwo))))
                .andExpect(status().isOk());
    }

    @Test
    void logoutEverywhereEndsEverySession() throws Exception {
        JsonNode first = register();
        JsonNode second = login();
        JsonNode third = login();

        mvc.perform(post("/auth/logout-all")
                        .header("Authorization", "Bearer " + first.get("accessToken").asText()))
                .andExpect(status().isNoContent());

        for (JsonNode session : new JsonNode[]{first, second, third}) {
            mvc.perform(post("/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("""
                                    {"refreshToken": "%s"}""".formatted(session.get("refreshToken").asText()))))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void rejectsLogoutEverywhereWithoutAToken() throws Exception {
        mvc.perform(post("/auth/logout-all"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsADuplicateEmail() throws Exception {
        register();

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationPayload()))
                .andExpect(status().isConflict());
    }

    @Test
    void rejectsAnInvalidRegistrationPayload() throws Exception {
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"user": {"email": "not-an-email", "firstName": "A", "phoneNumbers": []},
                                 "rawPassword": "short"}""")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isMap());
    }

    /**
     * The global catch-all sits behind every module advice. It must answer 500 without echoing the
     * exception message or a stack frame back to the caller.
     */
    @Test
    void answersAnUnmappedFailureWithAGeneric500() throws Exception {
        String accessToken = register().get("accessToken").asText();

        MvcResult result = mvc.perform(get("/__test__/boom")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andReturn();

        String payload = result.getResponse().getContentAsString();

        assertThat(payload)
                .doesNotContain(ExplodingController.SECRET)
                .doesNotContain("com.fiap.techchallenge");
    }

    @Test
    void storesRefreshTokensHashedOnly() throws Exception {
        String raw = register().get("refreshToken").asText();

        assertThat(refreshTokens.findAll())
                .isNotEmpty()
                .allSatisfy(token -> {
                    assertThat(token.getTokenHash()).isNotEqualTo(raw).hasSize(64);
                    assertThat(token.getTokenHash()).matches("[0-9a-f]{64}");
                });
    }

    private JsonNode register() throws Exception {
        MvcResult result = mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationPayload()))
                .andExpect(status().isCreated())
                .andReturn();

        // real users confirm via the emailed link; tests skip straight to the verified state
        userService.markEmailVerified(userService.findByEmail(email).orElseThrow().id());

        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode login() throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode refresh(String refreshToken) throws Exception {
        MvcResult result = mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("""
                                {"refreshToken": "%s"}""".formatted(refreshToken))))
                .andExpect(status().isOk())
                .andReturn();

        return json.readTree(result.getResponse().getContentAsString());
    }

    private String registrationPayload() {
        return body("""
                {
                  "user": {
                    "email": "%s",
                    "firstName": "Ana",
                    "lastName": "Souza",
                    "documentType": "CPF",
                    "documentCode": "%s",
                    "phoneNumbers": [{"type": "MOBILE", "phone": "11999999999", "isPrimary": true}]
                  },
                  "rawPassword": "%s"
                }""".formatted(email, document, PASSWORD));
    }

    private static String body(String raw) {
        return raw;
    }

    /**
     * A route no module claims, used to exercise the {@code Exception.class} catch-all.
     */
    @RestController
    static class ExplodingController {

        static final String SECRET = "leaky-internal-detail-42";

        @GetMapping("/__test__/boom")
        String boom() {
            throw new IllegalMonitorStateException(SECRET);
        }
    }

    @TestConfiguration
    static class ExplodingControllerConfiguration {

        @Bean
        ExplodingController explodingController() {
            return new ExplodingController();
        }
    }
}

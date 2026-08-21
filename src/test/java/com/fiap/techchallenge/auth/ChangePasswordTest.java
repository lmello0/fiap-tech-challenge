package com.fiap.techchallenge.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.user.api.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Rotation path for the password a bootstrap MANAGER is handed at startup — also exercises
 * {@code UserAuth.changePassword}, which otherwise has no caller.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ChangePasswordTest {

    private static final String PASSWORD = "aVeryLongPassword1";
    private static final String NEW_PASSWORD = "anotherVeryLongPassword2";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    final ObjectMapper json = new ObjectMapper();

    private String email;
    private String document;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().replace("-", "");

        this.email = unique.substring(0, 12) + "@example.com";
        this.document = uniqueDocument();
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

    @Test
    void changesThePasswordAndRevokesExistingSessions() throws Exception {
        JsonNode registration = register();
        String oldRefreshToken = registration.get("refreshToken").asText();
        String accessToken = registration.get("accessToken").asText();

        mvc.perform(post("/auth/password/change")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "%s"}""".formatted(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        // the old session no longer works
        mvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}""".formatted(oldRefreshToken)))
                .andExpect(status().isUnauthorized());

        // the old password no longer works
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isUnauthorized());

        // the new password does
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, NEW_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAWrongCurrentPassword() throws Exception {
        String accessToken = register().get("accessToken").asText();

        mvc.perform(post("/auth/password/change")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "totallyWrongPassword1", "newPassword": "%s"}""".formatted(NEW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void rejectsAnUnauthenticatedRequest() throws Exception {
        mvc.perform(post("/auth/password/change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "%s"}""".formatted(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode register() throws Exception {
        MvcResult result = mvc.perform(post("/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                }""".formatted(email, document, PASSWORD)))
                .andExpect(status().isCreated())
                .andReturn();

        userService.markEmailVerified(userService.findByEmail(email).orElseThrow().id());

        return json.readTree(result.getResponse().getContentAsString());
    }
}

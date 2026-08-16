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
        this.document = unique.replaceAll("\\D", "0").substring(0, 11);
    }

    @Test
    void changesThePasswordAndRevokesExistingSessions() throws Exception {
        JsonNode registration = register();
        String oldRefreshToken = registration.get("refreshToken").asText();
        String accessToken = registration.get("accessToken").asText();

        mvc.perform(post("/auth/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "%s"}""".formatted(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        // the old session no longer works
        mvc.perform(post("/auth/refresh")
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

        mvc.perform(post("/auth/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "totallyWrongPassword1", "newPassword": "%s"}""".formatted(NEW_PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid credentials"));
    }

    @Test
    void rejectsAnUnauthenticatedRequest() throws Exception {
        mvc.perform(post("/auth/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "%s"}""".formatted(PASSWORD, NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    private JsonNode register() throws Exception {
        MvcResult result = mvc.perform(post("/auth/register")
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

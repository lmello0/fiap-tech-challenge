package com.fiap.techchallenge.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
import com.fiap.techchallenge.user.api.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Password reset, email verification, and email change -- the token-issuing/consuming routes that
 * AuthEmailsTest deliberately doesn't reach (it only checks what AuthEmails hands to the email
 * module, never a real send). Raw tokens are only ever handed out via the confirmation email,
 * captured here through the same ApplicationEvents seam, mirroring AppointmentControllerTest's guest
 * token round trip.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class AuthEmailTokenFlowsTest {

    private static final String PASSWORD = "aVeryLongPassword1";
    private static final String NEW_PASSWORD = "anotherVeryLongPassword2";
    private static final Pattern TOKEN_PATTERN = Pattern.compile("(reset-password|verify-email|confirm-email-change)\\?token=(\\S+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    ApplicationEvents events;

    final ObjectMapper json = new ObjectMapper();

    private String email;
    private String document;

    @BeforeEach
    void setUp() {
        String unique = UUID.randomUUID().toString().replace("-", "");

        this.email = unique.substring(0, 12) + "@example.com";
        this.document = uniqueDocument();
    }

    @Test
    void aUserResetsTheirForgottenPassword() throws Exception {
        register();
        userService.markEmailVerified(userService.findByEmail(email).orElseThrow().id());

        mvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}""".formatted(email)))
                .andExpect(status().isNoContent());

        String token = tokenFor("reset-password");

        mvc.perform(post("/auth/password/reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s", "newPassword": "%s"}""".formatted(token, NEW_PASSWORD)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, NEW_PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void requestingAResetForAnUnknownEmailStillAnswers204() throws Exception {
        // masked, like registration's document/email-in-use checks elsewhere in this module: a 404
        // here would confirm which addresses are registered.
        mvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nobody-%s@example.com"}""".formatted(UUID.randomUUID())))
                .andExpect(status().isNoContent());
    }

    @Test
    void confirmingAPasswordResetWithAnInvalidTokenIsUnauthorized() throws Exception {
        mvc.perform(post("/auth/password/reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "not-a-real-token", "newPassword": "%s"}""".formatted(NEW_PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anUnverifiedAccountCannotLoginUntilEmailVerificationIsConfirmed() throws Exception {
        register();

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isForbidden());

        mvc.perform(post("/auth/email-verification/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}""".formatted(email)))
                .andExpect(status().isNoContent());

        // resend issues a fresh token; registration's own email is still sitting in the published
        // events too, but this pattern matches the resend deterministically all the same since both
        // point at the same verify-email path with a fresh token value.
        String token = tokenFor("verify-email");

        mvc.perform(post("/auth/email-verification/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s"}""".formatted(token)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void anAuthenticatedUserChangesTheirEmailAddress() throws Exception {
        JsonNode registration = register();
        userService.markEmailVerified(userService.findByEmail(email).orElseThrow().id());
        String accessToken = login(email).get("accessToken").asText();

        String newEmail = "changed-" + email;

        mvc.perform(post("/auth/email-change")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail": "%s"}""".formatted(newEmail)))
                .andExpect(status().isNoContent());

        // the old email still works until the change is confirmed (double opt-in, CONTEXT.md)
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, PASSWORD)))
                .andExpect(status().isOk());

        String token = tokenFor("confirm-email-change");

        mvc.perform(post("/auth/email-change/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token": "%s"}""".formatted(token)))
                .andExpect(status().isNoContent());

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(newEmail, PASSWORD)))
                .andExpect(status().isOk());
    }

    @Test
    void requestingAnEmailChangeWithoutAuthenticationIsUnauthorized() throws Exception {
        mvc.perform(post("/auth/email-change")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"newEmail": "someone-else@example.com"}"""))
                .andExpect(status().isUnauthorized());
    }

    // --- token extraction -----------------------------------------------------------------------

    private String tokenFor(String pathFragment) {
        List<EmailRequestedEvent> published = events.stream(EmailRequestedEvent.class).toList();

        for (int i = published.size() - 1; i >= 0; i--) {
            Matcher matcher = TOKEN_PATTERN.matcher(published.get(i).plainText());
            while (matcher.find()) {
                if (matcher.group(1).equals(pathFragment)) {
                    return matcher.group(2);
                }
            }
        }

        throw new AssertionError("No email link found for " + pathFragment
                + " among " + published.size() + " published email(s)");
    }

    // --- fixtures -------------------------------------------------------------------------------

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

        return json.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode login(String forEmail) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(forEmail, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();

        return json.readTree(result.getResponse().getContentAsString());
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
}

package com.fiap.techchallenge.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.api.AuthService;
import com.fiap.techchallenge.auth.api.commands.ConfirmPasswordResetCommand;
import com.fiap.techchallenge.auth.api.commands.RegisterCustomerCommand;
import com.fiap.techchallenge.auth.api.commands.RegisterWorkerCommand;
import com.fiap.techchallenge.auth.api.commands.RequestPasswordResetCommand;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.enums.AuthProvider;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
import com.fiap.techchallenge.email.api.EmailRequestedEvent;
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
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * An account whose first password was chosen by someone else can do exactly one thing until it
 * rotates that password. These tests pin both halves: that the flag is set on the accounts that
 * deserve it, and that no route — including the refresh endpoint — hands a flagged holder a way
 * around it.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@RecordApplicationEvents
class ForcedPasswordChangeTest {

    private static final String TEMPORARY_PASSWORD = "aTemporaryPassword1";
    private static final String CHOSEN_PASSWORD = "aChosenNewPassword1";

    private static final Pattern TOKEN_IN_LINK = Pattern.compile("token=(\\S+)");

    @Autowired
    MockMvc mvc;

    @Autowired
    AuthService authService;

    @Autowired
    UserService userService;

    @Autowired
    UserAuthRepository userAuths;

    @Autowired
    ApplicationEvents events;

    final ObjectMapper json = new ObjectMapper();

    @Test
    void onboardingAWorkerFlagsTheCredentialItCreates() {
        UserInfo worker = registerWorker();

        assertThat(needsPasswordChange(worker.id())).isTrue();
    }

    /**
     * The bootstrap manager is created through this same path (see BootstrapManagerRunner), so this
     * is what makes BOOTSTRAP_MANAGER_PASSWORD single-use.
     */
    @Test
    void aSelfRegisteredCustomerIsNotFlagged() {
        UserInfo customer = registerCustomer();

        assertThat(needsPasswordChange(customer.id())).isFalse();
    }

    @Test
    void aFlaggedTokenIsRefusedEverywhereButTheChangeEndpoint() throws Exception {
        String token = loginAsFlaggedWorker().get("accessToken").asText();

        mvc.perform(get("/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Password change required"));
    }

    @Test
    void aFlaggedTokenMayStillRotateItsPasswordAndIsFreeAfterwards() throws Exception {
        UserInfo worker = registerWorker();
        String token = login(worker.email(), TEMPORARY_PASSWORD).get("accessToken").asText();

        mvc.perform(post("/auth/password/change")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "%s", "newPassword": "%s"}"""
                                .formatted(TEMPORARY_PASSWORD, CHOSEN_PASSWORD)))
                .andExpect(status().isNoContent());

        assertThat(needsPasswordChange(worker.id())).isFalse();

        // A fresh login is required: changePassword revokes every refresh token, and the old access
        // token still carries the flag until it expires.
        String freed = login(worker.email(), CHOSEN_PASSWORD).get("accessToken").asText();

        mvc.perform(get("/users/me").header("Authorization", "Bearer " + freed))
                .andExpect(status().isOk());
    }

    /**
     * The hole worth guarding: refresh reissues an access token, so if it did not re-read the flag a
     * flagged holder could trade their way into a clean token without ever touching their password.
     */
    @Test
    void refreshingDoesNotLaunderAFlaggedToken() throws Exception {
        JsonNode session = loginAsFlaggedWorker();

        MvcResult refreshed = mvc.perform(post("/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken": "%s"}""".formatted(session.get("refreshToken").asText())))
                .andExpect(status().isOk())
                .andReturn();

        String reissued = json.readTree(refreshed.getResponse().getContentAsString())
                .get("accessToken").asText();

        mvc.perform(get("/users/me").header("Authorization", "Bearer " + reissued))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Password change required"));
    }

    /**
     * Someone who never learned their temporary password takes the emailed reset instead. It clears
     * the flag too, because both routes funnel through {@code UserAuth.changePassword}.
     */
    @Test
    void anEmailedPasswordResetAlsoClearsTheFlag() {
        UserInfo worker = registerWorker();

        authService.requestPasswordReset(new RequestPasswordResetCommand(worker.email()));
        authService.confirmPasswordReset(new ConfirmPasswordResetCommand(lastEmailedToken(), CHOSEN_PASSWORD));

        assertThat(needsPasswordChange(worker.id())).isFalse();
    }

    @Test
    void anUnauthenticatedRouteIsUntouchedByTheFilter() throws Exception {
        UserInfo worker = registerWorker();

        // No bearer token at all — the filter has no claim to read and must stay out of the way.
        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}"""
                                .formatted(worker.email(), TEMPORARY_PASSWORD)))
                .andExpect(status().isOk());
    }

    // --- fixtures -----------------------------------------------------------------------------

    private JsonNode loginAsFlaggedWorker() throws Exception {
        return login(registerWorker().email(), TEMPORARY_PASSWORD);
    }

    private UserInfo registerWorker() {
        UserInfo worker = authService.registerWorker(new RegisterWorkerCommand(
                new CreateWorkerCommand(
                        userCommand(),
                        WorkerRole.MECHANIC,
                        LocalDate.now().minusYears(1),
                        LocalDate.now().minusYears(1)),
                TEMPORARY_PASSWORD));

        userService.markEmailVerified(worker.id());
        events.clear();

        return worker;
    }

    private UserInfo registerCustomer() {
        CreateUserCommand user = userCommand();

        authService.registerCustomer(new RegisterCustomerCommand(user, CHOSEN_PASSWORD));

        return userService.findByEmail(user.email()).orElseThrow();
    }

    private JsonNode login(String email, String password) throws Exception {
        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "rawPassword": "%s"}""".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();

        return json.readTree(result.getResponse().getContentAsString());
    }

    private boolean needsPasswordChange(UUID userId) {
        return userAuths.findByUserIdAndProvider(userId, AuthProvider.LOCAL)
                .map(UserAuth::isNeedPasswordChange)
                .orElseThrow(() -> new AssertionError("No local credential for " + userId));
    }

    /**
     * Reads the raw token straight off the published email, the same way a recipient would read it
     * off the link. Auth never exposes it any other way — only the SHA-256 hash reaches a row.
     */
    private String lastEmailedToken() {
        List<EmailRequestedEvent> published = events.stream(EmailRequestedEvent.class).toList();

        assertThat(published).isNotEmpty();

        Matcher matcher = TOKEN_IN_LINK.matcher(published.getLast().plainText());

        assertThat(matcher.find()).isTrue();

        return matcher.group(1);
    }

    private CreateUserCommand userCommand() {
        return new CreateUserCommand(
                UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com",
                "Ana",
                "Souza",
                DocumentType.CPF,
                validCpf(),
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true)));
    }

    /** DocumentValidator rejects bad check digits, so the digits have to be computed, not random. */
    private static String validCpf() {
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

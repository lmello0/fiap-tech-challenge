package com.fiap.techchallenge.workorder.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.auth.entities.UserAuth;
import com.fiap.techchallenge.auth.repositories.UserAuthRepository;
import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Budget draft editing (add/remove/change-quantity line) and resend -- the parts of
 * BudgetController that WorkOrderAuthorizationTest's role-gating flow doesn't exercise (it only
 * reaches getById and send).
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class BudgetControllerTest {

    private static final String PASSWORD = "aVeryLongPassword1";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserService userService;

    @Autowired
    UserAuthRepository userAuths;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    RepairServiceCatalogService repairServiceCatalogService;

    final ObjectMapper json = new ObjectMapper();

    @Test
    void mechanicCanAddRemoveAndChangeLineQuantityWhileDraft() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        RepairServiceInfo first = createService();
        RepairServiceInfo second = createService();

        UUID budgetId = draftBudget(attendant, mechanic, customer, vehicleId, first);

        MvcResult addResult = mvc.perform(post("/budgets/{id}/lines", budgetId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "SERVICE", "quantity": 1, "serviceId": "%s"}""".formatted(second.id())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(2))
                .andReturn();

        UUID newLineId = UUID.fromString(json.readTree(addResult.getResponse().getContentAsString())
                .get("lines").get(1).get("id").asText());

        mvc.perform(patch("/budgets/{id}/lines/{lineId}/quantity", budgetId, newLineId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"quantity": 3}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines[1].quantity").value(3));

        mvc.perform(delete("/budgets/{id}/lines/{lineId}", budgetId, newLineId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lines.length()").value(1));
    }

    @Test
    void attendantCannotEditBudgetLines() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        RepairServiceInfo service = createService();
        UUID budgetId = draftBudget(attendant, mechanic, customer, vehicleId, service);

        mvc.perform(post("/budgets/{id}/lines", budgetId)
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "SERVICE", "quantity": 1, "serviceId": "%s"}""".formatted(service.id())))
                .andExpect(status().isForbidden());
    }

    @Test
    void editingALineOnASentBudgetIsAConflict() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        RepairServiceInfo service = createService();
        UUID budgetId = draftBudget(attendant, mechanic, customer, vehicleId, service);

        mvc.perform(post("/budgets/{id}/send", budgetId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/budgets/{id}/lines", budgetId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "SERVICE", "quantity": 1, "serviceId": "%s"}""".formatted(service.id())))
                .andExpect(status().isConflict());
    }

    @Test
    void attendantCanResendABudgetStuckInWaitingSend() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        RepairServiceInfo service = createService();
        UUID budgetId = draftBudget(attendant, mechanic, customer, vehicleId, service);

        mvc.perform(post("/budgets/{id}/send", budgetId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_SEND"));

        // still WAITING_SEND (delivery confirmation is async and not simulated here) -- resend is
        // exactly the "stuck after a failed delivery" recovery path.
        mvc.perform(post("/budgets/{id}/resend", budgetId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_SEND"));
    }

    @Test
    void mechanicCannotResendABudget() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        RepairServiceInfo service = createService();
        UUID budgetId = draftBudget(attendant, mechanic, customer, vehicleId, service);

        mvc.perform(post("/budgets/{id}/send", budgetId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/budgets/{id}/resend", budgetId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void addingALineWithANonPositiveQuantityIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

        RepairServiceInfo service = createService();
        UUID budgetId = draftBudget(attendant, mechanic, customer, vehicleId, service);

        mvc.perform(post("/budgets/{id}/lines", budgetId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type": "SERVICE", "quantity": 0, "serviceId": "%s"}""".formatted(service.id())))
                .andExpect(status().isBadRequest());
    }

    // --- flow fixture ---------------------------------------------------------------------------

    /** Drives a fresh work order through diagnostics up to a DRAFT budget with one SERVICE line. */
    private UUID draftBudget(
            Fixture attendant, Fixture mechanic, Fixture customer, UUID vehicleId, RepairServiceInfo service
    ) throws Exception {
        MvcResult createResult = mvc.perform(post("/work-orders")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "vehicleId": "%s",
                                  "complaint": "Something is wrong"
                                }""".formatted(customer.id(), vehicleId)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID workOrderId = UUID.fromString(json.readTree(createResult.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(post("/work-orders/{id}/diagnostics/request", workOrderId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/work-orders/{id}/diagnostics/start", workOrderId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"mechanicId": "%s"}""".formatted(mechanic.id())))
                .andExpect(status().isOk());

        MvcResult finishResult = mvc.perform(post("/work-orders/{id}/diagnostics/finish", workOrderId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diagnosis": "Needs work",
                                  "lines": [{"type": "SERVICE", "quantity": 1, "serviceId": "%s"}]
                                }""".formatted(service.id())))
                .andExpect(status().isOk())
                .andReturn();

        return UUID.fromString(json.readTree(finishResult.getResponse().getContentAsString()).get("budgetId").asText());
    }

    private RepairServiceInfo createService() {
        return repairServiceCatalogService.create(new CreateRepairServiceCommand(
                "BGT-SVC-" + UUID.randomUUID(), "Test Service", null, BigDecimal.valueOf(100), 1800));
    }

    private UUID createVehicle(Fixture owner) throws Exception {
        MvcResult result = mvc.perform(post("/vehicles")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": null,
                                  "vehicleType": "CAR",
                                  "licensePlate": "%s",
                                  "make": "Toyota",
                                  "model": "Corolla",
                                  "color": "Black",
                                  "modelYear": 2022,
                                  "manufactureYear": 2022,
                                  "fuelType": "FLEX",
                                  "transmissionType": "AUTOMATIC"
                                }""".formatted(uniquePlate())))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private static String uniquePlate() {
        return "BGT" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }

    // --- auth fixtures (mirrors WorkOrderAuthorizationTest.java) ----------------------------------

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

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
import com.fiap.techchallenge.workorder.entities.Budget;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.repositories.BudgetRepository;
import com.fiap.techchallenge.workorder.repositories.WorkOrderRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The staff-facing work order lifecycle past approval: repair execution through delivery, plus
 * getById/list and the Timeline reads. WorkOrderAuthorizationTest only reaches creation, diagnostics,
 * and the customer-view/Budget surface -- this covers the rest, which had no coverage at all.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class WorkOrderControllerTest {

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

    @Autowired
    WorkOrderRepository workOrderRepository;

    @Autowired
    BudgetRepository budgetRepository;

    final ObjectMapper json = new ObjectMapper();

    @Test
    void staffCanListAndGetWorkOrders() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID[] ids = approvedWorkOrder(attendant, mechanic);
        UUID workOrderId = ids[0];

        mvc.perform(get("/work-orders/{id}", workOrderId).header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(get("/work-orders")
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void gettingAnUnknownWorkOrderReturnsNotFound() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);

        mvc.perform(get("/work-orders/{id}", UUID.randomUUID()).header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void aCustomerCannotReachTheStaffWorkOrderSurface() throws Exception {
        Fixture customer = registerCustomer();

        mvc.perform(get("/work-orders").header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void mechanicDrivesRepairThroughToFinishAndStaffDeliversIt() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID[] ids = approvedWorkOrder(attendant, mechanic);
        UUID workOrderId = ids[0];
        UUID budgetId = ids[1];

        UUID lineId = firstLineId(mechanic, budgetId);

        mvc.perform(post("/work-orders/{id}/service/start", workOrderId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mvc.perform(post("/work-orders/{id}/lines/{lineId}/start", workOrderId, lineId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/work-orders/{id}/lines/{lineId}/finish", workOrderId, lineId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/work-orders/{id}/service/finish", workOrderId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHED"));

        mvc.perform(post("/work-orders/{id}/pickup-ready", workOrderId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_PICKUP"));

        mvc.perform(post("/work-orders/{id}/delivery", workOrderId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        assertThat(workOrderRepository.findById(workOrderId).orElseThrow().getStatus())
                .isEqualTo(WorkOrderStatus.DELIVERED);
    }

    @Test
    void anAttendantCannotStartOrFinishRepairLines() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID[] ids = approvedWorkOrder(attendant, mechanic);
        UUID workOrderId = ids[0];

        mvc.perform(post("/work-orders/{id}/service/start", workOrderId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isForbidden());
    }

    @Test
    void finishingALineTwiceIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID[] ids = approvedWorkOrder(attendant, mechanic);
        UUID workOrderId = ids[0];
        UUID budgetId = ids[1];

        UUID lineId = firstLineId(mechanic, budgetId);

        mvc.perform(post("/work-orders/{id}/service/start", workOrderId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/work-orders/{id}/lines/{lineId}/start", workOrderId, lineId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/work-orders/{id}/lines/{lineId}/finish", workOrderId, lineId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isOk());

        mvc.perform(post("/work-orders/{id}/lines/{lineId}/finish", workOrderId, lineId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void finishingDiagnosticsWithAPartLineMissingItsPartIdIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

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

        mvc.perform(post("/work-orders/{id}/diagnostics/finish", workOrderId)
                        .header("Authorization", "Bearer " + mechanic.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "diagnosis": "Needs work",
                                  "lines": [{"type": "PART", "quantity": 1}]
                                }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void finishingServiceBeforeStartingIsAConflict() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID[] ids = approvedWorkOrder(attendant, mechanic);
        UUID workOrderId = ids[0];

        mvc.perform(post("/work-orders/{id}/service/finish", workOrderId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void staffCanReadTheWorkOrdersTimelineAndASnapshot() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID[] ids = approvedWorkOrder(attendant, mechanic);
        UUID workOrderId = ids[0];

        MvcResult historyResult = mvc.perform(get("/work-orders/{id}/history", workOrderId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andReturn();

        UUID entryId = UUID.fromString(json.readTree(historyResult.getResponse().getContentAsString())
                .get("content").get(0).get("id").asText());

        mvc.perform(get("/work-orders/{id}/history/{entryId}", workOrderId, entryId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());
    }

    @Test
    void aHistorySnapshotForAnUnknownEntryIsNotFound() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID[] ids = approvedWorkOrder(attendant, mechanic);
        UUID workOrderId = ids[0];

        mvc.perform(get("/work-orders/{id}/history/{entryId}", workOrderId, UUID.randomUUID())
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void startingALineBeforeServiceHasStartedIsAConflict() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture mechanic = registerWorker(WorkerRole.MECHANIC);
        UUID[] ids = approvedWorkOrder(attendant, mechanic);
        UUID workOrderId = ids[0];
        UUID budgetId = ids[1];

        UUID lineId = firstLineId(mechanic, budgetId);

        // APPROVED, not yet IN_PROGRESS (service/start was never called)
        mvc.perform(post("/work-orders/{id}/lines/{lineId}/start", workOrderId, lineId)
                        .header("Authorization", "Bearer " + mechanic.accessToken()))
                .andExpect(status().isConflict());
    }

    @Test
    void creatingAWorkOrderForAVehicleOwnedBySomeoneElseIsRejected() throws Exception {
        Fixture attendant = registerWorker(WorkerRole.ATTENDANT);
        Fixture vehicleOwner = registerCustomer();
        Fixture otherCustomer = registerCustomer();
        UUID vehicleId = createVehicle(vehicleOwner);

        mvc.perform(post("/work-orders")
                        .header("Authorization", "Bearer " + attendant.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerId": "%s",
                                  "vehicleId": "%s",
                                  "complaint": "Something is wrong"
                                }""".formatted(otherCustomer.id(), vehicleId)))
                .andExpect(status().isBadRequest());
    }

    // --- flow fixture ---------------------------------------------------------------------------

    /** Drives a fresh work order all the way to APPROVED, returning {workOrderId, budgetId}. */
    private UUID[] approvedWorkOrder(Fixture attendant, Fixture mechanic) throws Exception {
        RepairServiceInfo service = repairServiceCatalogService.create(new CreateRepairServiceCommand(
                "WO-SVC-" + UUID.randomUUID(), "Test Service", null, BigDecimal.valueOf(100), 1800));

        Fixture customer = registerCustomer();
        UUID vehicleId = createVehicle(customer);

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
        UUID budgetId = UUID.fromString(json.readTree(finishResult.getResponse().getContentAsString()).get("budgetId").asText());

        mvc.perform(post("/budgets/{id}/send", budgetId)
                        .header("Authorization", "Bearer " + attendant.accessToken()))
                .andExpect(status().isOk());

        confirmDelivery(budgetId);

        mvc.perform(post("/work-orders/{id}/customer-view/budget/approval", workOrderId)
                        .param("budgetId", budgetId.toString())
                        .header("Authorization", "Bearer " + customer.accessToken()))
                .andExpect(status().isOk());

        return new UUID[]{workOrderId, budgetId};
    }

    private UUID firstLineId(Fixture caller, UUID budgetId) throws Exception {
        MvcResult result = mvc.perform(get("/budgets/{id}", budgetId)
                        .header("Authorization", "Bearer " + caller.accessToken()))
                .andReturn();

        return UUID.fromString(json.readTree(result.getResponse().getContentAsString())
                .get("lines").get(0).get("id").asText());
    }

    private void confirmDelivery(UUID budgetId) {
        Budget budget = budgetRepository.findById(budgetId).orElseThrow();
        budget.setStatus(BudgetStatus.SENT);
        budget.setSentAt(Instant.now());
        budgetRepository.save(budget);

        WorkOrder wo = workOrderRepository.findById(budget.getWorkOrderId()).orElseThrow();
        wo.setStatus(WorkOrderStatus.WAITING_APPROVAL);
        workOrderRepository.save(wo);
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
        return "WOC" + ThreadLocalRandom.current().nextInt(1000, 9999);
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

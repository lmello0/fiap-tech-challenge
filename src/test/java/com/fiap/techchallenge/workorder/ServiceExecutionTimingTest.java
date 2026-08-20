package com.fiap.techchallenge.workorder;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.workorder.api.BudgetService;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.AddBudgetLineCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.representation.BudgetInfo;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import com.fiap.techchallenge.workorder.entities.Budget;
import com.fiap.techchallenge.workorder.entities.BudgetLine;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import com.fiap.techchallenge.workorder.enums.RowType;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.exceptions.WorkOrderNotInProgressException;
import com.fiap.techchallenge.workorder.repositories.BudgetRepository;
import com.fiap.techchallenge.workorder.repositories.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the per-service execution timing wired into WorkOrderServiceImpl.startLine/finishLine: a
 * service's quoted duration falls back to its seeded estimate until samples exist, then becomes a
 * rolling average over the most recent app.inventory.average-window (20) samples — not an all-time
 * average, which is what the last test in this class specifically checks.
 *
 * <p>Budgets are driven straight to SENT/APPROVED via the repository rather than through the real
 * async email delivery-confirmation flow — see {@code PartReservationFlowTest} for why.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ServiceExecutionTimingTest {

    @Autowired
    WorkOrderService workOrderService;

    @Autowired
    BudgetService budgetService;

    @Autowired
    UserService userService;

    @Autowired
    WorkOrderRepository workOrderRepository;

    @Autowired
    BudgetRepository budgetRepository;

    @Autowired
    RepairServiceCatalogService repairServiceCatalogService;

    @Test
    void averageSecondsFallsBackToTheEstimateUntilASampleExists() {
        RepairServiceInfo service = createService("TIMING-SEED-1", 1800);

        assertThat(service.averageSeconds()).isEqualTo(1800);
    }

    @Test
    void finishingALineRecordsASampleAndUpdatesTheRollingAverage() {
        RepairServiceInfo service = createService("TIMING-AVG-1", 1800);

        recordExecution(service.id(), 100);
        recordExecution(service.id(), 200);
        recordExecution(service.id(), 300);

        RepairServiceInfo afterThree = repairServiceCatalogService.getById(service.id());
        assertThat(afterThree.averageSeconds()).isEqualTo(200);
        assertThat(afterThree.executionCount()).isEqualTo(3);
    }

    @Test
    void averageIsOverTheMostRecentWindowNotAllTime() {
        RepairServiceInfo service = createService("TIMING-WINDOW-1", 1800);

        // 25 samples of 1..25 seconds. The average-window is 20, so only the most recent 20
        // (durations 6..25, mean 15.5, rounds to 16) should count — not all 25 (mean 13).
        for (int duration = 1; duration <= 25; duration++) {
            recordExecution(service.id(), duration);
        }

        RepairServiceInfo afterTwentyFive = repairServiceCatalogService.getById(service.id());
        assertThat(afterTwentyFive.executionCount()).isEqualTo(25);
        assertThat(afterTwentyFive.averageSeconds()).isEqualTo(16);
    }

    @Test
    void lineLifecycleGuardsRejectDoubleStartAndFinishBeforeStart() {
        RepairServiceInfo service = createService("TIMING-GUARD-1", 1800);
        UUID workOrderId = seedWorkOrder();
        advanceToInProgress(workOrderId, service.id());

        UUID lineId = firstLineId(workOrderId);

        assertThatThrownBy(() -> workOrderService.finishLine(workOrderId, lineId))
                .isInstanceOf(IllegalArgumentException.class);

        workOrderService.startLine(workOrderId, lineId);

        assertThatThrownBy(() -> workOrderService.startLine(workOrderId, lineId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startingALineOnAWorkOrderThatIsNotInProgressIsRejected() {
        RepairServiceInfo service = createService("TIMING-GUARD-2", 1800);
        UUID workOrderId = seedWorkOrder();

        advanceToInDiagnostics(workOrderId);
        WorkOrderInfo quoted = finishDiagnosticsWith(workOrderId, service.id());
        UUID lineId = firstLineId(workOrderId);

        // Still BUDGET_IN_DRAFT, not IN_PROGRESS.
        assertThat(quoted.status()).isEqualTo(WorkOrderStatus.BUDGET_IN_DRAFT);
        assertThatThrownBy(() -> workOrderService.startLine(workOrderId, lineId))
                .isInstanceOf(WorkOrderNotInProgressException.class);
    }

    // --- fixtures -----------------------------------------------------------------------------

    /**
     * Runs one full line through start -> finish, backdating startedAt so the elapsed time is
     * exactly {@code durationSeconds} rather than depending on a real sleep.
     */
    private void recordExecution(UUID serviceId, int durationSeconds) {
        UUID workOrderId = seedWorkOrder();
        advanceToInProgress(workOrderId, serviceId);

        UUID lineId = firstLineId(workOrderId);

        workOrderService.startLine(workOrderId, lineId);
        backdateLineStart(workOrderId, lineId, durationSeconds);
        workOrderService.finishLine(workOrderId, lineId);
    }

    private UUID firstLineId(UUID workOrderId) {
        WorkOrder wo = workOrderRepository.findById(workOrderId).orElseThrow();
        Budget budget = budgetRepository.findWithLinesById(wo.getBudgetId()).orElseThrow();

        return budget.getLines().get(0).getId();
    }

    private void backdateLineStart(UUID workOrderId, UUID lineId, int durationSeconds) {
        WorkOrder wo = workOrderRepository.findById(workOrderId).orElseThrow();
        Budget budget = budgetRepository.findWithLinesById(wo.getBudgetId()).orElseThrow();
        BudgetLine line = budget.getLines().stream().filter(l -> l.getId().equals(lineId)).findFirst().orElseThrow();
        line.setStartedAt(Instant.now().minusSeconds(durationSeconds));
        budgetRepository.save(budget);
    }

    private void advanceToInProgress(UUID workOrderId, UUID serviceId) {
        advanceToInDiagnostics(workOrderId);
        WorkOrderInfo quoted = finishDiagnosticsWith(workOrderId, serviceId);
        UUID budgetId = quoted.budgetId();

        sendAndConfirm(budgetId);
        budgetService.approve(budgetId, quoted.customerId());

        workOrderService.startService(workOrderId);
    }

    private void advanceToInDiagnostics(UUID workOrderId) {
        workOrderService.requestDiagnostics(workOrderId);
        workOrderService.startDiagnostics(workOrderId, new StartDiagnosticsCommand(UUID.randomUUID()));
    }

    private WorkOrderInfo finishDiagnosticsWith(UUID workOrderId, UUID serviceId) {
        return workOrderService.finishDiagnostics(workOrderId, new FinishDiagnosticsCommand(
                "Diagnosed",
                List.of(new AddBudgetLineCommand(RowType.SERVICE, BigDecimal.ONE, null, serviceId))
        ));
    }

    /** Simulates confirmed email delivery without a real SMTP server: DRAFT -> WAITING_SEND -> SENT. */
    private void sendAndConfirm(UUID budgetId) {
        budgetService.send(budgetId);

        Budget budget = budgetRepository.findById(budgetId).orElseThrow();
        budget.setStatus(BudgetStatus.SENT);
        budget.setSentAt(Instant.now());
        budgetRepository.save(budget);

        WorkOrder wo = workOrderRepository.findById(budget.getWorkOrderId()).orElseThrow();
        wo.setStatus(WorkOrderStatus.WAITING_APPROVAL);
        workOrderRepository.save(wo);
    }

    private UUID seedWorkOrder() {
        WorkOrder wo = new WorkOrder();
        wo.setOrderCode("WO-TIMING-" + UUID.randomUUID().toString().substring(0, 8));
        wo.setStatus(WorkOrderStatus.RECEIVED);
        // A real, registered customer: dispatchBudgetEmail (send/resend) looks the customer up via
        // UserService to find their email, so a bare random UUID would 404 there.
        wo.setCustomerId(registerCustomer());
        wo.setVehicleId(UUID.randomUUID());
        wo.setAssignedMechanicId(UUID.randomUUID());

        return workOrderRepository.save(wo).getId();
    }

    private UUID registerCustomer() {
        String email = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";

        return userService.createCustomer(new com.fiap.techchallenge.user.api.commands.CreateUserCommand(
                email,
                "Test",
                "Customer",
                com.fiap.techchallenge.user.enums.DocumentType.CPF,
                uniqueDocument(),
                java.util.List.of(new com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand(
                        com.fiap.techchallenge.user.enums.PhoneType.MOBILE, "11999999999", true))
        )).id();
    }

    /** DocumentValidator rejects bad check digits, so the digits have to be computed, not random. */
    private static String uniqueDocument() {
        int[] digits = new int[11];

        for (int i = 0; i < 9; i++) {
            digits[i] = java.util.concurrent.ThreadLocalRandom.current().nextInt(10);
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

    private RepairServiceInfo createService(String code, int estimatedSeconds) {
        return repairServiceCatalogService.create(new CreateRepairServiceCommand(
                code, "Test Service " + code, null, BigDecimal.valueOf(100), estimatedSeconds));
    }
}

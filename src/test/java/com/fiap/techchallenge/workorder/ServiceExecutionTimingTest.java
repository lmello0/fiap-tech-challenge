package com.fiap.techchallenge.workorder;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderRowCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderRowInfo;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.entities.WorkOrderRow;
import com.fiap.techchallenge.workorder.enums.RowType;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.exceptions.WorkOrderNotInProgressException;
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
 * Exercises the per-service execution timing wired into WorkOrderServiceImpl.startRow/finishRow: a
 * service's quoted duration falls back to its seeded estimate until samples exist, then becomes a
 * rolling average over the most recent app.inventory.average-window (20) samples — not an all-time
 * average, which is what the last test in this class specifically checks.
 *
 * WorkOrder fixtures are seeded directly via the repository, for the same reason as
 * PartReservationFlowTest: WorkOrderService.create() never sets assignedMechanicId, which the DB
 * requires NOT NULL.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ServiceExecutionTimingTest {

    @Autowired
    WorkOrderService workOrderService;

    @Autowired
    WorkOrderRepository workOrderRepository;

    @Autowired
    RepairServiceCatalogService repairServiceCatalogService;

    @Test
    void averageSecondsFallsBackToTheEstimateUntilASampleExists() {
        RepairServiceInfo service = createService("TIMING-SEED-1", 1800);

        assertThat(service.averageSeconds()).isEqualTo(1800);
    }

    @Test
    void finishingARowRecordsASampleAndUpdatesTheRollingAverage() {
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
    void rowLifecycleGuardsRejectDoubleStartAndFinishBeforeStart() {
        RepairServiceInfo service = createService("TIMING-GUARD-1", 1800);
        UUID workOrderId = seedWorkOrder();
        advanceToInProgress(workOrderId, service.id());

        WorkOrderInfo wo = workOrderService.getWorkOrderById(workOrderId);
        UUID rowId = wo.rows().get(0).id();

        assertThatThrownBy(() -> workOrderService.finishRow(workOrderId, rowId))
                .isInstanceOf(IllegalArgumentException.class);

        workOrderService.startRow(workOrderId, rowId);

        assertThatThrownBy(() -> workOrderService.startRow(workOrderId, rowId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startingARowOnAWorkOrderThatIsNotInProgressIsRejected() {
        RepairServiceInfo service = createService("TIMING-GUARD-2", 1800);
        UUID workOrderId = seedWorkOrder();

        advanceToInDiagnostics(workOrderId);
        WorkOrderInfo quoted = finishDiagnosticsWith(workOrderId, service.id());
        UUID rowId = quoted.rows().get(0).id();

        // Still WAITING_APPROVAL, not IN_PROGRESS.
        assertThatThrownBy(() -> workOrderService.startRow(workOrderId, rowId))
                .isInstanceOf(WorkOrderNotInProgressException.class);
    }

    // --- fixtures -----------------------------------------------------------------------------

    /**
     * Runs one full row through start -> finish, backdating startedAt so the elapsed time is exactly
     * {@code durationSeconds} rather than depending on a real sleep.
     */
    private void recordExecution(UUID serviceId, int durationSeconds) {
        UUID workOrderId = seedWorkOrder();
        advanceToInProgress(workOrderId, serviceId);

        WorkOrderInfo wo = workOrderService.getWorkOrderById(workOrderId);
        UUID rowId = wo.rows().get(0).id();

        workOrderService.startRow(workOrderId, rowId);
        backdateRowStart(workOrderId, rowId, durationSeconds);
        workOrderService.finishRow(workOrderId, rowId);
    }

    private void backdateRowStart(UUID workOrderId, UUID rowId, int durationSeconds) {
        WorkOrder wo = workOrderRepository.findById(workOrderId).orElseThrow();
        WorkOrderRow row = wo.getRows().stream().filter(r -> r.getId().equals(rowId)).findFirst().orElseThrow();
        row.setStartedAt(Instant.now().minusSeconds(durationSeconds));
        workOrderRepository.save(wo);
    }

    private void advanceToInProgress(UUID workOrderId, UUID serviceId) {
        advanceToInDiagnostics(workOrderId);
        finishDiagnosticsWith(workOrderId, serviceId);
        workOrderService.approve(workOrderId);
        workOrderService.startService(workOrderId);
    }

    private void advanceToInDiagnostics(UUID workOrderId) {
        workOrderService.requestDiagnostics(workOrderId);
        workOrderService.startDiagnostics(workOrderId, new StartDiagnosticsCommand(UUID.randomUUID()));
    }

    private WorkOrderInfo finishDiagnosticsWith(UUID workOrderId, UUID serviceId) {
        return workOrderService.finishDiagnostics(workOrderId, new FinishDiagnosticsCommand(
                "Diagnosed",
                List.of(new CreateWorkOrderRowCommand(RowType.SERVICE, BigDecimal.ONE, null, serviceId))
        ));
    }

    private UUID seedWorkOrder() {
        WorkOrder wo = new WorkOrder();
        wo.setOrderCode("WO-TIMING-" + UUID.randomUUID());
        wo.setStatus(WorkOrderStatus.RECEIVED);
        wo.setCustomerId(UUID.randomUUID());
        wo.setVehicleId(UUID.randomUUID());
        wo.setAssignedMechanicId(UUID.randomUUID());

        return workOrderRepository.save(wo).getId();
    }

    private RepairServiceInfo createService(String code, int estimatedSeconds) {
        return repairServiceCatalogService.create(new CreateRepairServiceCommand(
                code, "Test Service " + code, null, BigDecimal.valueOf(100), estimatedSeconds));
    }
}

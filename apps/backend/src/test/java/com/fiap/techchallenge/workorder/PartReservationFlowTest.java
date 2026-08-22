package com.fiap.techchallenge.workorder;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.inventory.api.PartCatalogService;
import com.fiap.techchallenge.inventory.api.PartReservationService;
import com.fiap.techchallenge.inventory.api.RepairServiceCatalogService;
import com.fiap.techchallenge.inventory.api.StockService;
import com.fiap.techchallenge.inventory.api.commands.AdjustStockCommand;
import com.fiap.techchallenge.inventory.api.commands.CreatePartCommand;
import com.fiap.techchallenge.inventory.api.commands.CreateRepairServiceCommand;
import com.fiap.techchallenge.inventory.api.representation.PartInfo;
import com.fiap.techchallenge.inventory.api.representation.RepairServiceInfo;
import com.fiap.techchallenge.inventory.entities.PartReservation;
import com.fiap.techchallenge.inventory.enums.ReservationStatus;
import com.fiap.techchallenge.inventory.enums.StockMovementType;
import com.fiap.techchallenge.inventory.enums.UnitOfMeasure;
import com.fiap.techchallenge.inventory.exceptions.InsufficientStockException;
import com.fiap.techchallenge.inventory.repositories.PartReservationRepository;
import com.fiap.techchallenge.inventory.repositories.StockMovementRepository;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.workorder.api.BudgetService;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.AddBudgetLineCommand;
import com.fiap.techchallenge.workorder.api.commands.FinishDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.commands.RefuseWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import com.fiap.techchallenge.workorder.entities.Budget;
import com.fiap.techchallenge.workorder.entities.WorkOrder;
import com.fiap.techchallenge.workorder.enums.BudgetStatus;
import com.fiap.techchallenge.workorder.enums.RowType;
import com.fiap.techchallenge.workorder.enums.WorkOrderStatus;
import com.fiap.techchallenge.workorder.repositories.BudgetRepository;
import com.fiap.techchallenge.workorder.repositories.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the reservation lifecycle wired into the Budget draft/approve flow: best-effort reserve
 * at finishDiagnostics, release at refuse, strict consume at startService, and the age-based expiry
 * sweep. WorkOrder fixtures are seeded directly via the repository rather than through
 * WorkOrderService.create() — that method now also calls out to the vehicle module synchronously,
 * which these tests don't need to exercise.
 *
 * <p>Budgets are driven straight to SENT via the repository rather than through the real async email
 * delivery-confirmation flow (BudgetServiceImpl#send publishes an EmailRequestedEvent and only
 * transitions to SENT once EmailDeliveredEvent arrives) — there's no SMTP server in this test slice.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class PartReservationFlowTest {

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
    PartCatalogService partCatalogService;

    @Autowired
    RepairServiceCatalogService repairServiceCatalogService;

    @Autowired
    StockService stockService;

    @Autowired
    PartReservationService reservationService;

    @Autowired
    PartReservationRepository reservationRepository;

    @Autowired
    StockMovementRepository movementRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void quotingReservesWhatExistsAndRecordsAShortfall() {
        PartInfo part = createPart("RES-SHORT-1", 2);
        RepairServiceInfo service = createService("SVC-SHORT-1");
        UUID workOrderId = seedWorkOrder();

        advanceToInDiagnostics(workOrderId);
        WorkOrderInfo afterQuote = finishDiagnosticsWith(workOrderId, part.id(), BigDecimal.valueOf(4), service.id());

        assertThat(afterQuote.status()).isEqualTo(WorkOrderStatus.BUDGET_IN_DRAFT);

        PartInfo afterReserve = partCatalogService.getById(part.id());
        assertThat(afterReserve.quantityReserved()).isEqualByComparingTo("2");
        assertThat(afterReserve.available()).isEqualByComparingTo("0");

        List<PartReservation> reservations =
                reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.HELD);
        assertThat(reservations).hasSize(1);
        assertThat(reservations.get(0).getShortfall()).isEqualByComparingTo("2");

        UUID budgetId = afterQuote.budgetId();
        sendAndConfirm(budgetId);
        approve(budgetId, afterQuote.customerId());

        assertThatThrownBy(() -> workOrderService.startService(workOrderId))
                .isInstanceOf(InsufficientStockException.class);
    }

    @Test
    void refusalReleasesTheReservationAndWritesNoStockMovement() {
        PartInfo part = createPart("RES-REFUSE-1", 5);
        RepairServiceInfo service = createService("SVC-REFUSE-1");
        UUID workOrderId = seedWorkOrder();

        advanceToInDiagnostics(workOrderId);
        WorkOrderInfo afterQuote = finishDiagnosticsWith(workOrderId, part.id(), BigDecimal.valueOf(3), service.id());

        assertThat(partCatalogService.getById(part.id()).quantityReserved()).isEqualByComparingTo("3");

        UUID budgetId = afterQuote.budgetId();
        sendAndConfirm(budgetId);
        budgetService.refuse(budgetId, afterQuote.customerId(), new RefuseWorkOrderCommand("Too expensive"));

        PartInfo afterRefusal = partCatalogService.getById(part.id());
        assertThat(afterRefusal.quantityReserved()).isEqualByComparingTo("0");
        assertThat(afterRefusal.quantityOnHand()).isEqualByComparingTo("5");

        List<PartReservation> released =
                reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.RELEASED);
        assertThat(released).hasSize(1);

        boolean anyConsumptionForThisOrder = movementRepository.findByPartIdOrderByOccurredAtDesc(part.id(), Pageable.unpaged())
                .stream()
                .noneMatch(m -> m.getType() == StockMovementType.CONSUMPTION);
        assertThat(anyConsumptionForThisOrder).isTrue();

        assertThat(workOrderRepository.findById(workOrderId).orElseThrow().getStatus())
                .isEqualTo(WorkOrderStatus.REFUSED);
    }

    @Test
    void startingServiceConsumesTheReservationAndWritesAConsumptionMovement() {
        PartInfo part = createPart("RES-CONSUME-1", 5);
        RepairServiceInfo service = createService("SVC-CONSUME-1");
        UUID workOrderId = seedWorkOrder();

        advanceToInDiagnostics(workOrderId);
        WorkOrderInfo afterQuote = finishDiagnosticsWith(workOrderId, part.id(), BigDecimal.valueOf(3), service.id());

        UUID budgetId = afterQuote.budgetId();
        sendAndConfirm(budgetId);
        approve(budgetId, afterQuote.customerId());

        WorkOrderInfo started = workOrderService.startService(workOrderId);
        assertThat(started.status()).isEqualTo(WorkOrderStatus.IN_PROGRESS);

        PartInfo afterConsume = partCatalogService.getById(part.id());
        assertThat(afterConsume.quantityOnHand()).isEqualByComparingTo("2");
        assertThat(afterConsume.quantityReserved()).isEqualByComparingTo("0");

        List<PartReservation> consumed =
                reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.CONSUMED);
        assertThat(consumed).hasSize(1);

        boolean hasConsumptionMovement = movementRepository.findByPartIdOrderByOccurredAtDesc(part.id(), Pageable.unpaged())
                .stream()
                .anyMatch(m -> m.getType() == StockMovementType.CONSUMPTION
                        && m.getQuantity().compareTo(BigDecimal.valueOf(-3)) == 0
                        && workOrderId.equals(m.getReferenceId()));
        assertThat(hasConsumptionMovement).isTrue();
    }

    @Test
    void expirySweepReleasesReservationsPastTheirTtlAndSkipsRecentOnes() {
        PartInfo part = createPart("RES-EXPIRE-1", 5);
        RepairServiceInfo service = createService("SVC-EXPIRE-1");
        UUID workOrderId = seedWorkOrder();

        advanceToInDiagnostics(workOrderId);
        finishDiagnosticsWith(workOrderId, part.id(), BigDecimal.valueOf(3), service.id());

        PartReservation reservation =
                reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.HELD).get(0);

        // reserved_at is @CreationTimestamp/updatable=false, so backdating it has to go around
        // Hibernate directly rather than through the entity setter, which it would silently ignore.
        jdbcTemplate.update(
                "UPDATE inventory.part_reservations SET reserved_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minus(Duration.ofDays(10))), reservation.getId());

        int expired = reservationService.expireStaleReservations(Instant.now().minus(Duration.ofDays(7)));

        assertThat(expired).isEqualTo(1);
        assertThat(partCatalogService.getById(part.id()).quantityReserved()).isEqualByComparingTo("0");
        assertThat(reservationRepository.findByWorkOrderIdAndStatus(workOrderId, ReservationStatus.EXPIRED))
                .hasSize(1);
    }

    // --- fixtures -----------------------------------------------------------------------------

    private PartInfo createPart(String sku, int onHand) {
        PartInfo part = partCatalogService.create(new CreatePartCommand(
                sku, "Test Part " + sku, null, null, UnitOfMeasure.UNIT, BigDecimal.valueOf(50)));

        if (onHand > 0) {
            stockService.adjust(part.id(), new AdjustStockCommand(BigDecimal.valueOf(onHand), "Seed"));
        }

        return partCatalogService.getById(part.id());
    }

    private RepairServiceInfo createService(String code) {
        return repairServiceCatalogService.create(new CreateRepairServiceCommand(
                code, "Test Service " + code, null, BigDecimal.valueOf(100), 1800));
    }

    private UUID seedWorkOrder() {
        WorkOrder wo = new WorkOrder();
        wo.setOrderCode("WO-TEST-" + UUID.randomUUID().toString().substring(0, 8));
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

    private void advanceToInDiagnostics(UUID workOrderId) {
        workOrderService.requestDiagnostics(workOrderId);
        workOrderService.startDiagnostics(workOrderId, new StartDiagnosticsCommand(UUID.randomUUID()));
    }

    private WorkOrderInfo finishDiagnosticsWith(UUID workOrderId, UUID partId, BigDecimal partQuantity, UUID serviceId) {
        return workOrderService.finishDiagnostics(workOrderId, new FinishDiagnosticsCommand(
                "Diagnosed",
                List.of(
                        new AddBudgetLineCommand(RowType.PART, partQuantity, partId, null),
                        new AddBudgetLineCommand(RowType.SERVICE, BigDecimal.ONE, null, serviceId)
                )
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

    private void approve(UUID budgetId, UUID customerId) {
        budgetService.approve(budgetId, customerId);
    }
}

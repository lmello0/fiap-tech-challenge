package com.fiap.techchallenge.history;

import com.fiap.techchallenge.TestcontainersConfiguration;
import com.fiap.techchallenge.history.api.HistoryQueryService;
import com.fiap.techchallenge.history.api.representation.HistoryEntryInfo;
import com.fiap.techchallenge.history.api.representation.HistorySnapshotInfo;
import com.fiap.techchallenge.history.entities.HistoryEntry;
import com.fiap.techchallenge.history.repositories.HistoryEntryRepository;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.enums.DocumentType;
import com.fiap.techchallenge.user.enums.PhoneType;
import com.fiap.techchallenge.user.enums.WorkerRole;
import com.fiap.techchallenge.vehicle.api.VehicleService;
import com.fiap.techchallenge.vehicle.api.commands.CreateVehicleCommand;
import com.fiap.techchallenge.vehicle.enums.FuelType;
import com.fiap.techchallenge.vehicle.enums.TransmissionType;
import com.fiap.techchallenge.vehicle.enums.VehicleType;
import com.fiap.techchallenge.workorder.api.WorkOrderService;
import com.fiap.techchallenge.workorder.api.commands.CreateWorkOrderCommand;
import com.fiap.techchallenge.workorder.api.commands.StartDiagnosticsCommand;
import com.fiap.techchallenge.workorder.api.events.WorkOrderAggregate;
import com.fiap.techchallenge.workorder.api.representation.WorkOrderInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * End-to-end: driving a real Work Order through its early lifecycle produces a Timeline nobody wrote
 * to directly (ADR 0011/0012). Written against {@link HistoryQueryService} — the same surface
 * {@code WorkOrderController}/{@code CustomerWorkOrderController} use — plus the internal repository,
 * to also prove customer-visible filtering and the append-only trigger from inside the module that
 * owns both.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class HistoryFlowTest {

    @Autowired
    UserService userService;

    @Autowired
    VehicleService vehicleService;

    @Autowired
    WorkOrderService workOrderService;

    @Autowired
    HistoryQueryService historyQueryService;

    @Autowired
    HistoryEntryRepository historyEntryRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void workOrderLifecycleProducesATimelineWithCustomerVisibleFiltering() {
        UUID customerId = registerCustomer();
        UUID vehicleId = registerVehicle(customerId);

        WorkOrderInfo wo = workOrderService.create(
                new CreateWorkOrderCommand(customerId, vehicleId, "Strange noise on braking"));
        workOrderService.requestDiagnostics(wo.id());
        workOrderService.startDiagnostics(wo.id(), new StartDiagnosticsCommand(registerMechanic()));

        // Async: HistoryEntryWriter runs after each publishing transaction commits, on another thread.
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(historyEntryRepository
                        .findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
                                WorkOrderAggregate.TYPE, wo.id(), Pageable.unpaged()))
                        .hasSize(3));

        List<HistoryEntryInfo> staffTimeline = historyQueryService
                .timeline(WorkOrderAggregate.TYPE, wo.id(), Pageable.unpaged())
                .getContent();
        assertThat(staffTimeline)
                .extracting(HistoryEntryInfo::eventType)
                .containsExactlyInAnyOrder("WORK_ORDER_CREATED", "WORK_ORDER_DIAGNOSTICS_REQUESTED",
                        "WORK_ORDER_DIAGNOSTICS_STARTED");
        assertThat(staffTimeline).allSatisfy(entry -> {
            assertThat(entry.occurredAt()).isNotNull();
            assertThat(entry.actorType()).isNotNull();
        });

        // WORK_ORDER_DIAGNOSTICS_STARTED is not customer-visible (Q27): the customer doesn't need to
        // know which mechanic was assigned before the budget exists.
        List<HistoryEntryInfo> customerTimeline = historyQueryService
                .customerVisibleTimeline(WorkOrderAggregate.TYPE, wo.id(), Pageable.unpaged())
                .getContent();
        assertThat(customerTimeline)
                .extracting(HistoryEntryInfo::eventType)
                .containsExactlyInAnyOrder("WORK_ORDER_CREATED", "WORK_ORDER_DIAGNOSTICS_REQUESTED");
        assertThat(customerTimeline).allSatisfy(entry -> {
            assertThat(entry.actorId()).isNull();
            assertThat(entry.actorLabel()).isNull();
        });

        HistoryEntryInfo createdEntry = staffTimeline.stream()
                .filter(e -> e.eventType().equals("WORK_ORDER_CREATED"))
                .findFirst().orElseThrow();

        HistorySnapshotInfo snapshot = historyQueryService
                .snapshot(createdEntry.id(), WorkOrderAggregate.TYPE, wo.id())
                .orElseThrow();
        assertThat(snapshot.schemaVersion()).isEqualTo(1);
        assertThat(snapshot.snapshot().get("workOrder").get("orderCode").asText()).isEqualTo(wo.orderCode());
        assertThat(snapshot.snapshot().get("workOrder").get("status").asText()).isEqualTo("RECEIVED");

        // A customer reaching for the staff-only entry through the customer-scoped read gets nothing.
        HistoryEntryInfo startedEntry = staffTimeline.stream()
                .filter(e -> e.eventType().equals("WORK_ORDER_DIAGNOSTICS_STARTED"))
                .findFirst().orElseThrow();
        assertThat(historyQueryService.customerVisibleSnapshot(startedEntry.id(), WorkOrderAggregate.TYPE, wo.id()))
                .isEmpty();
    }

    @Test
    void anEntryCanNeverBeRewrittenOrRemoved() {
        UUID customerId = registerCustomer();
        UUID vehicleId = registerVehicle(customerId);
        WorkOrderInfo wo = workOrderService.create(
                new CreateWorkOrderCommand(customerId, vehicleId, "Append-only check"));

        HistoryEntry entry = await().atMost(Duration.ofSeconds(5)).until(() ->
                        historyEntryRepository.findByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(
                                WorkOrderAggregate.TYPE, wo.id(), Pageable.unpaged()).stream().findFirst(),
                java.util.Optional::isPresent
        ).orElseThrow();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE history.entry SET event_type = 'TAMPERED' WHERE id = ?", entry.getId()))
                .isInstanceOf(DataAccessException.class);

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM history.entry WHERE id = ?", entry.getId()))
                .isInstanceOf(DataAccessException.class);
    }

    private UUID registerCustomer() {
        String email = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";

        return userService.createCustomer(new CreateUserCommand(
                email,
                "History",
                "Tester",
                DocumentType.CPF,
                uniqueDocument(),
                List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true))
        )).id();
    }

    private UUID registerMechanic() {
        String email = UUID.randomUUID().toString().replace("-", "").substring(0, 12) + "@example.com";

        return userService.createWorker(new CreateWorkerCommand(
                new CreateUserCommand(
                        email,
                        "History",
                        "Mechanic",
                        DocumentType.CPF,
                        uniqueDocument(),
                        List.of(new RegisterPhoneNumberCommand(PhoneType.MOBILE, "11999999999", true))
                ),
                WorkerRole.MECHANIC,
                LocalDate.now().minusYears(1),
                LocalDate.now().minusYears(1)
        )).id();
    }

    private UUID registerVehicle(UUID customerId) {
        return vehicleService.create(new CreateVehicleCommand(
                customerId,
                VehicleType.CAR,
                uniquePlate(),
                "Toyota",
                "Corolla",
                "Black",
                2022,
                2022,
                null,
                FuelType.FLEX,
                TransmissionType.AUTOMATIC
        )).id();
    }

    private static String uniquePlate() {
        return "HST" + ThreadLocalRandom.current().nextInt(1000, 9999);
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

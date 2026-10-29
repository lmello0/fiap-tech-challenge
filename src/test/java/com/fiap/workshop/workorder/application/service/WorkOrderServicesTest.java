package com.fiap.workshop.workorder.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.shared.application.PageQuery;
import com.fiap.workshop.shared.application.RecordingEventPublisher;
import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.shared.domain.ConflictException;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.NotFoundException;
import com.fiap.workshop.workorder.api.events.BudgetApproved;
import com.fiap.workshop.workorder.api.events.BudgetIssued;
import com.fiap.workshop.workorder.api.events.BudgetRejected;
import com.fiap.workshop.workorder.api.events.WorkOrderCancelled;
import com.fiap.workshop.workorder.api.events.WorkOrderCompleted;
import com.fiap.workshop.workorder.api.events.WorkOrderDelivered;
import com.fiap.workshop.workorder.api.events.WorkOrderOpened;
import com.fiap.workshop.workorder.api.events.WorkOrderStatusChanged;
import com.fiap.workshop.workorder.application.port.in.AddItemUseCase;
import com.fiap.workshop.workorder.application.port.in.ApproveBudgetUseCase;
import com.fiap.workshop.workorder.application.port.in.ChangeItemQuantityUseCase;
import com.fiap.workshop.workorder.application.port.in.ItemRequest;
import com.fiap.workshop.workorder.application.port.in.OpenWorkOrderUseCase;
import com.fiap.workshop.workorder.application.port.in.ProposeAdditionalBudgetUseCase;
import com.fiap.workshop.workorder.application.port.in.RejectBudgetUseCase;
import com.fiap.workshop.workorder.application.port.out.CustomerLookup.CustomerInfo;
import com.fiap.workshop.workorder.application.port.out.CustomerLookup.VehicleInfo;
import com.fiap.workshop.workorder.application.port.out.ItemCatalog.ItemInfo;
import com.fiap.workshop.workorder.application.port.out.WorkOrderRepository.WorkOrderFilter;
import com.fiap.workshop.workorder.domain.BudgetStatus;
import com.fiap.workshop.workorder.domain.DecisionChannel;
import com.fiap.workshop.workorder.domain.ItemType;
import com.fiap.workshop.workorder.domain.WorkOrder;
import com.fiap.workshop.workorder.domain.WorkOrderStateException;
import com.fiap.workshop.workorder.domain.WorkOrderStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WorkOrderServicesTest {

	private static final Actor ATTENDANT = new Actor("att-1", "ATTENDANT");
	private static final Actor MECHANIC = new Actor("mec-1", "MECHANIC");
	private static final Actor CUSTOMER = new Actor("cus-1", "CUSTOMER");

	private final Clock clock = Clock.fixed(Instant.parse("2026-06-01T12:00:00Z"), ZoneOffset.UTC);
	private final Fakes.Repository repository = new Fakes.Repository();
	private final Fakes.Customers customers = new Fakes.Customers();
	private final Fakes.Catalog catalog = new Fakes.Catalog();
	private final RecordingEventPublisher events = new RecordingEventPublisher();
	private final WorkOrderSupport support = new WorkOrderSupport(repository, events, clock);
	private final ItemResolver resolver = new ItemResolver(catalog);

	private final OpenWorkOrderService open = new OpenWorkOrderService(customers, resolver, new Fakes.Numbers(), repository, support);
	private final AddItemService addItem = new AddItemService(support, repository, resolver);
	private final ChangeItemQuantityService changeQuantity = new ChangeItemQuantityService(support, repository);
	private final RemoveItemService removeItem = new RemoveItemService(support, repository);
	private final StartDiagnosisService startDiagnosis = new StartDiagnosisService(support);
	private final CompleteDiagnosisService completeDiagnosis = new CompleteDiagnosisService(support);
	private final ProposeAdditionalBudgetService additional = new ProposeAdditionalBudgetService(support, resolver);
	private final ApproveBudgetService approve = new ApproveBudgetService(support);
	private final RejectBudgetService reject = new RejectBudgetService(support);
	private final CompleteWorkOrderService complete = new CompleteWorkOrderService(support);
	private final DeliverWorkOrderService deliver = new DeliverWorkOrderService(support);
	private final CancelWorkOrderService cancel = new CancelWorkOrderService(support);

	private CustomerInfo ana;
	private VehicleInfo argo;
	private ItemInfo oilChange;
	private ItemInfo oilFilter;

	@BeforeEach
	void setUp() {
		ana = new CustomerInfo(UUID.randomUUID(), "Ana Souza", "52998224725", "ana@example.com", true);
		argo = new VehicleInfo(UUID.randomUUID(), ana.id(), "ABC1D23", "Fiat", "Argo", 2022, true);
		customers.byDocument.put(ana.document(), ana);
		customers.byPlate.put(argo.plate(), argo);
		oilChange = catalog.add(ItemType.SERVICE, "Oil change", "150", true);
		oilFilter = catalog.add(ItemType.PART, "Oil filter", "35", true);
	}

	private WorkOrder openOrder(ItemRequest... items) {
		return open.execute(new OpenWorkOrderUseCase.Command(ana.document(), argo.plate(), "noise", List.of(items), ATTENDANT));
	}

	private ItemRequest service(int quantity) {
		return new ItemRequest(ItemType.SERVICE, oilChange.id(), quantity);
	}

	private ItemRequest part(int quantity) {
		return new ItemRequest(ItemType.PART, oilFilter.id(), quantity);
	}

	private WorkOrder waitingApproval(ItemRequest... items) {
		WorkOrder wo = openOrder(items);
		startDiagnosis.execute(wo.id(), MECHANIC);
		return completeDiagnosis.execute(wo.id(), MECHANIC);
	}

	private UUID budgetId(WorkOrder wo) {
		return wo.budgets().getLast().id();
	}

	@Test
	void opensWorkOrderWithSnapshotsAndPublishesEvents() {
		WorkOrder wo = openOrder(service(1), part(2));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.RECEIVED);
		assertThat(wo.number()).isEqualTo("OS-2026-000001");
		assertThat(wo.customer().name()).isEqualTo("Ana Souza");
		assertThat(wo.vehicle().plate()).isEqualTo("ABC1D23");
		assertThat(wo.draftItems()).extracting("name").containsExactly("Oil change", "Oil filter");
		assertThat(events.of(WorkOrderOpened.class)).singleElement().extracting(WorkOrderOpened::workOrderId).isEqualTo(wo.id());
		assertThat(events.of(WorkOrderStatusChanged.class)).singleElement().satisfies(e -> {
			assertThat(e.fromStatus()).isNull();
			assertThat(e.toStatus()).isEqualTo("RECEIVED");
			assertThat(e.actorSubject()).isEqualTo("att-1");
		});
	}

	@Test
	void numbersAreSequential() {
		WorkOrder first = openOrder();
		first.cancel("x", ATTENDANT, clock.instant());
		repository.save(first);

		assertThat(openOrder().number()).isEqualTo("OS-2026-000002");
	}

	@Test
	void refusesToOpenForUnknownOrInactiveCustomerOrVehicle() {
		assertThatThrownBy(() -> open.execute(new OpenWorkOrderUseCase.Command("11144477735", argo.plate(), null, List.of(), ATTENDANT)))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> open.execute(new OpenWorkOrderUseCase.Command(ana.document(), "ZZZ9999", null, List.of(), ATTENDANT)))
				.isInstanceOf(NotFoundException.class);

		customers.byDocument.put(ana.document(), new CustomerInfo(ana.id(), ana.name(), ana.document(), ana.email(), false));
		assertThatThrownBy(this::openOrder).isInstanceOf(DomainException.class).hasMessageContaining("deactivated");

		customers.byDocument.put(ana.document(), ana);
		customers.byPlate.put(argo.plate(), new VehicleInfo(argo.id(), ana.id(), argo.plate(), "Fiat", "Argo", 2022, false));
		assertThatThrownBy(this::openOrder).isInstanceOf(DomainException.class).hasMessageContaining("deactivated");
		assertThat(repository.store).isEmpty();
	}

	@Test
	void refusesAVehicleThatBelongsToSomeoneElse() {
		customers.byPlate.put(argo.plate(), new VehicleInfo(argo.id(), UUID.randomUUID(), argo.plate(), "Fiat", "Argo", 2022, true));

		assertThatThrownBy(this::openOrder).isInstanceOf(DomainException.class).hasMessageContaining("does not belong");
	}

	@Test
	void refusesASecondActiveWorkOrderForTheSameVehicle() {
		openOrder();

		assertThatThrownBy(this::openOrder).isInstanceOf(ConflictException.class);
	}

	@Test
	void refusesUnknownOrInactiveItems() {
		ItemInfo retired = catalog.add(ItemType.SERVICE, "Retired", "10", false);

		assertThatThrownBy(() -> openOrder(new ItemRequest(ItemType.SERVICE, UUID.randomUUID(), 1))).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> openOrder(new ItemRequest(ItemType.SERVICE, retired.id(), 1))).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> openOrder(new ItemRequest(ItemType.PART, oilChange.id(), 1))).isInstanceOf(NotFoundException.class);
		assertThat(repository.store).isEmpty();
	}

	@Test
	void editsDraftItems() {
		WorkOrder wo = openOrder(service(1));

		wo = addItem.execute(new AddItemUseCase.Command(wo.id(), part(2)));
		assertThat(wo.draftItems()).hasSize(2);
		UUID filterItem = wo.draftItems().getLast().id();
		wo = changeQuantity.execute(new ChangeItemQuantityUseCase.Command(wo.id(), filterItem, 5));
		assertThat(wo.draftItems().getLast().quantity()).isEqualTo(5);
		wo = removeItem.execute(wo.id(), filterItem);
		assertThat(wo.draftItems()).hasSize(1);
		assertThatThrownBy(() -> addItem.execute(new AddItemUseCase.Command(UUID.randomUUID(), part(1)))).isInstanceOf(NotFoundException.class);
	}

	@Test
	void diagnosisIssuesTheBudgetAndPublishesIt() {
		WorkOrder wo = waitingApproval(service(1), part(2));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.WAITING_APPROVAL);
		assertThat(events.of(BudgetIssued.class)).singleElement().satisfies(e -> {
			assertThat(e.workOrderId()).isEqualTo(wo.id());
			assertThat(e.kind()).isEqualTo("INITIAL");
			assertThat(e.total()).isEqualByComparingTo("220");
			assertThat(e.customerEmail()).isEqualTo("ana@example.com");
			assertThat(e.lines()).hasSize(2);
		});
		assertThat(events.of(WorkOrderStatusChanged.class)).extracting(WorkOrderStatusChanged::toStatus)
				.containsExactly("RECEIVED", "IN_DIAGNOSIS", "WAITING_APPROVAL");
	}

	@Test
	void customerApprovalWithoutPartsStartsExecution() {
		WorkOrder wo = waitingApproval(service(1));

		WorkOrder approved = approve.execute(new ApproveBudgetUseCase.Command(wo.id(), budgetId(wo), CUSTOMER, null, ana.id()));

		assertThat(approved.status()).isEqualTo(WorkOrderStatus.IN_EXECUTION);
		assertThat(approved.budgets().getFirst().decision().channel()).isEqualTo(DecisionChannel.APP);
		assertThat(events.of(BudgetApproved.class)).singleElement().satisfies(e -> {
			assertThat(e.channel()).isEqualTo("APP");
			assertThat(e.parts()).isEmpty();
		});
	}

	@Test
	void approvalWithPartsPublishesThePartsAndWaitsForStock() {
		WorkOrder wo = waitingApproval(service(1), part(3));

		WorkOrder approved = approve.execute(new ApproveBudgetUseCase.Command(wo.id(), budgetId(wo), ATTENDANT, DecisionChannel.PHONE, null));

		assertThat(approved.status()).isEqualTo(WorkOrderStatus.WAITING_APPROVAL);
		assertThat(approved.budgets().getFirst().status()).isEqualTo(BudgetStatus.APPROVED);
		assertThat(events.of(BudgetApproved.class)).singleElement().satisfies(e -> {
			assertThat(e.channel()).isEqualTo("PHONE");
			assertThat(e.parts()).singleElement().satisfies(p -> {
				assertThat(p.partId()).isEqualTo(oilFilter.id());
				assertThat(p.quantity()).isEqualTo(3);
			});
		});
	}

	@Test
	void staffMustSayWhichChannelTheCustomerUsed() {
		WorkOrder wo = waitingApproval(service(1));

		assertThatThrownBy(() -> approve.execute(new ApproveBudgetUseCase.Command(wo.id(), budgetId(wo), ATTENDANT, null, null)))
				.isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> approve.execute(new ApproveBudgetUseCase.Command(wo.id(), budgetId(wo), ATTENDANT, DecisionChannel.APP, null)))
				.isInstanceOf(DomainException.class);
		assertThat(repository.store.get(wo.id()).status()).isEqualTo(WorkOrderStatus.WAITING_APPROVAL);
	}

	@Test
	void aCustomerCannotSeeOrDecideSomeoneElsesWorkOrder() {
		WorkOrder wo = waitingApproval(service(1));
		UUID stranger = UUID.randomUUID();

		assertThatThrownBy(() -> approve.execute(new ApproveBudgetUseCase.Command(wo.id(), budgetId(wo), CUSTOMER, null, stranger)))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> reject.execute(new RejectBudgetUseCase.Command(wo.id(), budgetId(wo), CUSTOMER, null, "no", stranger)))
				.isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> new GetWorkOrderService(support).execute(wo.id(), stranger)).isInstanceOf(NotFoundException.class);
		assertThat(new GetWorkOrderService(support).execute(wo.id(), ana.id())).isSameAs(wo);
		assertThat(new GetWorkOrderService(support).execute(wo.id(), null)).isSameAs(wo);
	}

	@Test
	void rejectionReturnsToDiagnosisAndKeepsTheReason() {
		WorkOrder wo = waitingApproval(service(1), part(1));

		WorkOrder rejected = reject.execute(new RejectBudgetUseCase.Command(wo.id(), budgetId(wo), CUSTOMER, null, "  too expensive ", ana.id()));

		assertThat(rejected.status()).isEqualTo(WorkOrderStatus.IN_DIAGNOSIS);
		assertThat(rejected.draftItems()).hasSize(2);
		assertThat(events.of(BudgetRejected.class)).singleElement().extracting(BudgetRejected::reason).isEqualTo("too expensive");
	}

	@Test
	void blankRejectionReasonIsStoredAsNull() {
		WorkOrder wo = waitingApproval(service(1));

		reject.execute(new RejectBudgetUseCase.Command(wo.id(), budgetId(wo), CUSTOMER, null, "  ", ana.id()));

		assertThat(events.of(BudgetRejected.class)).singleElement().extracting(BudgetRejected::reason).isNull();
	}

	@Test
	void fullLifecycleWithAdditionalBudgetCompletionAndDelivery() {
		WorkOrder wo = waitingApproval(service(1));
		approve.execute(new ApproveBudgetUseCase.Command(wo.id(), budgetId(wo), CUSTOMER, null, ana.id()));

		WorkOrder extra = additional.execute(new ProposeAdditionalBudgetUseCase.Command(wo.id(), List.of(part(2)), MECHANIC));
		assertThat(extra.status()).isEqualTo(WorkOrderStatus.WAITING_APPROVAL);
		assertThat(events.of(BudgetIssued.class)).extracting(BudgetIssued::kind).containsExactly("INITIAL", "ADDITIONAL");

		reject.execute(new RejectBudgetUseCase.Command(wo.id(), budgetId(extra), CUSTOMER, null, "no thanks", ana.id()));
		assertThat(repository.store.get(wo.id()).status()).isEqualTo(WorkOrderStatus.IN_EXECUTION);

		complete.execute(wo.id(), MECHANIC);
		deliver.execute(wo.id(), ATTENDANT);

		assertThat(repository.store.get(wo.id()).status()).isEqualTo(WorkOrderStatus.DELIVERED);
		assertThat(events.of(WorkOrderCompleted.class)).singleElement().extracting(WorkOrderCompleted::parts).isEqualTo(List.of());
		assertThat(events.of(WorkOrderDelivered.class)).hasSize(1);
		assertThat(openOrder().number()).isEqualTo("OS-2026-000002");
	}

	@Test
	void completionReportsOnlyApprovedParts() {
		WorkOrder wo = waitingApproval(service(1), part(2));
		approve.execute(new ApproveBudgetUseCase.Command(wo.id(), budgetId(wo), ATTENDANT, DecisionChannel.IN_PERSON, null));
		repository.store.get(wo.id()).startExecution(Actor.SYSTEM, clock.instant());

		complete.execute(wo.id(), MECHANIC);

		assertThat(events.of(WorkOrderCompleted.class)).singleElement().satisfies(e -> {
			assertThat(e.parts()).singleElement().extracting("quantity").isEqualTo(2);
			assertThat(e.customerName()).isEqualTo("Ana Souza");
		});
	}

	@Test
	void cancellationPublishesTheReasonAndFreesTheVehicle() {
		WorkOrder wo = waitingApproval(service(1));

		cancel.execute(wo.id(), "customer gave up", ATTENDANT);

		assertThat(repository.store.get(wo.id()).status()).isEqualTo(WorkOrderStatus.CANCELLED);
		assertThat(events.of(WorkOrderCancelled.class)).singleElement().extracting(WorkOrderCancelled::reason).isEqualTo("customer gave up");
		assertThat(openOrder()).isNotNull();
		assertThatThrownBy(() -> cancel.execute(wo.id(), "again", ATTENDANT)).isInstanceOf(WorkOrderStateException.class);
	}

	@Test
	void invalidTransitionsAreRejectedWithoutSideEffects() {
		WorkOrder wo = openOrder(service(1));
		int published = events.events().size();

		assertThatThrownBy(() -> completeDiagnosis.execute(wo.id(), MECHANIC)).isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> complete.execute(wo.id(), MECHANIC)).isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> deliver.execute(wo.id(), ATTENDANT)).isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> startDiagnosis.execute(UUID.randomUUID(), MECHANIC)).isInstanceOf(NotFoundException.class);
		assertThat(events.events()).hasSize(published);
	}

	@Test
	void listsAndFiltersByCustomerAndStatus() {
		WorkOrder wo = openOrder(service(1));
		var list = new ListWorkOrdersService(repository);

		assertThat(list.execute(new WorkOrderFilter(ana.id(), null, null, null), new PageQuery(0, 10)).content())
				.singleElement().extracting("number").isEqualTo(wo.number());
		assertThat(list.execute(new WorkOrderFilter(UUID.randomUUID(), null, null, null), new PageQuery(0, 10)).content()).isEmpty();
		assertThat(list.execute(new WorkOrderFilter(null, WorkOrderStatus.DELIVERED, null, null), new PageQuery(0, 10)).totalElements()).isZero();
	}

	@Test
	void snapshotRefreshUpdatesOnlyWhatIsStillOpen() {
		WorkOrder wo = openOrder(service(1));
		var refresh = new RefreshSnapshotsService(repository);

		refresh.refreshCustomer(ana.id(), "Ana Lima", ana.document(), "lima@example.com");
		refresh.refreshVehicle(argo.id(), "ABC1D23", "VW", "Polo", 2023);
		refresh.refreshService(oilChange.id(), "Oil change plus", new BigDecimal("170"));
		refresh.refreshService(UUID.randomUUID(), "Unrelated", BigDecimal.ONE);
		refresh.refreshPart(oilFilter.id(), "Whatever", BigDecimal.TEN);

		WorkOrder stored = repository.store.get(wo.id());
		assertThat(stored.customer().name()).isEqualTo("Ana Lima");
		assertThat(stored.customer().email()).isEqualTo("lima@example.com");
		assertThat(stored.vehicle().model()).isEqualTo("Polo");
		assertThat(stored.draftItems().getFirst().name()).isEqualTo("Oil change plus");
		assertThat(stored.draftItems().getFirst().unitPrice().amount()).isEqualByComparingTo("170");

		cancel.execute(wo.id(), "x", ATTENDANT);
		refresh.refreshCustomer(ana.id(), "Changed Again", ana.document(), "again@example.com");
		assertThat(repository.store.get(wo.id()).customer().name()).isEqualTo("Ana Lima");
	}

	@Test
	void snapshotRefreshNeverTouchesAnIssuedBudget() {
		WorkOrder wo = waitingApproval(service(1));

		new RefreshSnapshotsService(repository).refreshService(oilChange.id(), "Oil change gold", new BigDecimal("999"));

		assertThat(repository.store.get(wo.id()).budgets().getFirst().lines().getFirst().unitPrice().amount()).isEqualByComparingTo("150");
	}
}

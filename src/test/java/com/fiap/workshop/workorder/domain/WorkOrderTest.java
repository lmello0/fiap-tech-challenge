package com.fiap.workshop.workorder.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fiap.workshop.shared.domain.Actor;
import com.fiap.workshop.shared.domain.DomainException;
import com.fiap.workshop.shared.domain.Money;
import com.fiap.workshop.shared.domain.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class WorkOrderTest {

	private static final Instant T0 = Instant.parse("2026-06-01T10:00:00Z");
	private static final Actor ATTENDANT = new Actor("att-1", "ATTENDANT");
	private static final Actor MECHANIC = new Actor("mec-1", "MECHANIC");
	private static final Actor CUSTOMER = new Actor("cus-1", "CUSTOMER");

	private final UUID oilChange = UUID.randomUUID();
	private final UUID filter = UUID.randomUUID();
	private final UUID pads = UUID.randomUUID();

	private WorkOrder open() {
		return WorkOrder.open("OS-2026-000001",
				new CustomerSnapshot(UUID.randomUUID(), "Ana Souza", "52998224725", "ana@example.com"),
				new VehicleSnapshot(UUID.randomUUID(), "ABC1D23", "Fiat", "Argo", 2022), "  noise when braking ",
				List.of(new NewItem(ItemType.SERVICE, oilChange, "Oil change", Money.of("150"), 1)), ATTENDANT, T0);
	}

	private WorkOrder inDiagnosis() {
		WorkOrder wo = open();
		wo.startDiagnosis(MECHANIC, T0.plusSeconds(60));
		return wo;
	}

	private WorkOrder waitingApproval(boolean withParts) {
		WorkOrder wo = inDiagnosis();
		if (withParts) {
			wo.addItem(ItemType.PART, filter, "Oil filter", Money.of("35"), 2);
		}
		wo.completeDiagnosis(MECHANIC, T0.plusSeconds(120));
		return wo;
	}

	private Decision approval(Actor actor, DecisionChannel channel) {
		return new Decision(actor, channel, null, T0.plusSeconds(180));
	}

	@Test
	void opensReceivedWithItemsAndFirstHistoryEntry() {
		WorkOrder wo = open();

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.RECEIVED);
		assertThat(wo.complaint()).isEqualTo("noise when braking");
		assertThat(wo.draftItems()).singleElement().extracting(WorkOrderItem::name).isEqualTo("Oil change");
		assertThat(wo.budgets()).isEmpty();
		assertThat(wo.history()).singleElement().satisfies(change -> {
			assertThat(change.from()).isNull();
			assertThat(change.to()).isEqualTo(WorkOrderStatus.RECEIVED);
			assertThat(change.actor()).isEqualTo(ATTENDANT);
		});
	}

	@Test
	void addingTheSameItemTwiceMergesQuantities() {
		WorkOrder wo = open();

		wo.addItem(ItemType.SERVICE, oilChange, "Oil change", Money.of("150"), 2);

		assertThat(wo.draftItems()).singleElement().extracting(WorkOrderItem::quantity).isEqualTo(3);
	}

	@Test
	void sameReferenceOfDifferentTypesAreDifferentItems() {
		WorkOrder wo = open();

		wo.addItem(ItemType.PART, oilChange, "Weird part", Money.of("1"), 1);

		assertThat(wo.draftItems()).hasSize(2);
	}

	@Test
	void changesAndRemovesItems() {
		WorkOrder wo = open();
		UUID itemId = wo.draftItems().getFirst().id();

		wo.changeItemQuantity(itemId, 4);
		assertThat(wo.draftItems().getFirst().subtotal()).isEqualTo(Money.of("600"));
		wo.removeItem(itemId);
		assertThat(wo.draftItems()).isEmpty();
		assertThatThrownBy(() -> wo.removeItem(itemId)).isInstanceOf(NotFoundException.class);
		assertThatThrownBy(() -> wo.changeItemQuantity(UUID.randomUUID(), 1)).isInstanceOf(NotFoundException.class);
	}

	@Test
	void rejectsInvalidQuantities() {
		WorkOrder wo = open();
		UUID itemId = wo.draftItems().getFirst().id();

		assertThatThrownBy(() -> wo.changeItemQuantity(itemId, 0)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> wo.changeItemQuantity(itemId, 1000)).isInstanceOf(DomainException.class);
		assertThatThrownBy(() -> wo.addItem(ItemType.PART, filter, "F", Money.of("1"), -1)).isInstanceOf(DomainException.class);
	}

	@Test
	void completingDiagnosisFreezesItemsIntoAPendingBudget() {
		WorkOrder wo = inDiagnosis();
		wo.addItem(ItemType.PART, filter, "Oil filter", Money.of("35"), 2);

		Budget budget = wo.completeDiagnosis(MECHANIC, T0.plusSeconds(120));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.WAITING_APPROVAL);
		assertThat(wo.draftItems()).isEmpty();
		assertThat(budget.sequence()).isEqualTo(1);
		assertThat(budget.kind()).isEqualTo(BudgetKind.INITIAL);
		assertThat(budget.status()).isEqualTo(BudgetStatus.PENDING);
		assertThat(budget.previousBudgetId()).isNull();
		assertThat(budget.total()).isEqualTo(Money.of("220"));
		assertThat(budget.lines()).hasSize(2);
		assertThat(budget.parts()).containsExactly(new PartQuantity(filter, 2));
		assertThat(wo.budgets()).containsExactly(budget);
	}

	@Test
	void itemsAreLockedOnceTheBudgetIsIssued() {
		WorkOrder wo = waitingApproval(false);

		assertThatThrownBy(() -> wo.addItem(ItemType.SERVICE, UUID.randomUUID(), "X", Money.of("1"), 1))
				.isInstanceOf(WorkOrderStateException.class);
	}

	@Test
	void diagnosisNeedsItemsAndTheRightStatus() {
		WorkOrder wo = inDiagnosis();
		wo.removeItem(wo.draftItems().getFirst().id());

		assertThatThrownBy(() -> wo.completeDiagnosis(MECHANIC, T0)).isInstanceOf(DomainException.class).hasMessageContaining("at least one item");
		assertThatThrownBy(() -> open().completeDiagnosis(MECHANIC, T0)).isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> waitingApproval(false).startDiagnosis(MECHANIC, T0)).isInstanceOf(WorkOrderStateException.class);
	}

	@Test
	void approvingABudgetWithoutPartsStartsExecution() {
		WorkOrder wo = waitingApproval(false);
		Budget budget = wo.budgets().getFirst();

		wo.approveBudget(budget.id(), approval(CUSTOMER, DecisionChannel.APP), T0.plusSeconds(180));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.IN_EXECUTION);
		assertThat(budget.status()).isEqualTo(BudgetStatus.APPROVED);
		assertThat(budget.decision().actor()).isEqualTo(CUSTOMER);
		assertThat(budget.decision().channel()).isEqualTo(DecisionChannel.APP);
		assertThat(wo.approvedTotal()).isEqualTo(Money.of("150"));
		assertThat(wo.history().getLast().actor()).isEqualTo(CUSTOMER);
	}

	@Test
	void approvingABudgetWithPartsWaitsForTheStockOutcome() {
		WorkOrder wo = waitingApproval(true);

		wo.approveBudget(wo.budgets().getFirst().id(), approval(ATTENDANT, DecisionChannel.PHONE), T0.plusSeconds(180));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.WAITING_APPROVAL);
		assertThat(wo.awaitingStock()).isTrue();
		assertThat(wo.approvedParts()).containsExactly(new PartQuantity(filter, 2));
	}

	@Test
	void stockOutcomeMovesTheOrderToExecutionOrWaitingForParts() {
		WorkOrder reserved = waitingApproval(true);
		reserved.approveBudget(reserved.budgets().getFirst().id(), approval(CUSTOMER, DecisionChannel.APP), T0);
		reserved.startExecution(Actor.SYSTEM, T0.plusSeconds(200));
		assertThat(reserved.status()).isEqualTo(WorkOrderStatus.IN_EXECUTION);

		WorkOrder short1 = waitingApproval(true);
		short1.approveBudget(short1.budgets().getFirst().id(), approval(CUSTOMER, DecisionChannel.APP), T0);
		short1.waitForParts(Actor.SYSTEM, T0.plusSeconds(200));
		assertThat(short1.status()).isEqualTo(WorkOrderStatus.WAITING_PARTS);
		assertThat(short1.awaitingStock()).isFalse();
		short1.startExecution(Actor.SYSTEM, T0.plusSeconds(300));
		assertThat(short1.status()).isEqualTo(WorkOrderStatus.IN_EXECUTION);
	}

	@Test
	void stockOutcomeIsRejectedWhenNothingIsWaiting() {
		WorkOrder wo = waitingApproval(true);

		assertThatThrownBy(() -> wo.startExecution(Actor.SYSTEM, T0)).isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> wo.waitForParts(Actor.SYSTEM, T0)).isInstanceOf(WorkOrderStateException.class);
	}

	@Test
	void rejectingTheInitialBudgetReturnsToDiagnosisWithItemsRestored() {
		WorkOrder wo = waitingApproval(true);
		Budget first = wo.budgets().getFirst();

		wo.rejectBudget(first.id(), new Decision(CUSTOMER, DecisionChannel.APP, "too expensive", T0), T0.plusSeconds(180));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.IN_DIAGNOSIS);
		assertThat(first.status()).isEqualTo(BudgetStatus.REJECTED);
		assertThat(first.decision().reason()).isEqualTo("too expensive");
		assertThat(wo.draftItems()).extracting(WorkOrderItem::name).containsExactly("Oil change", "Oil filter");

		wo.removeItem(wo.draftItems().getLast().id());
		Budget second = wo.completeDiagnosis(MECHANIC, T0.plusSeconds(240));
		assertThat(second.sequence()).isEqualTo(2);
		assertThat(second.previousBudgetId()).isEqualTo(first.id());
		assertThat(second.total()).isEqualTo(Money.of("150"));
		assertThat(wo.approvedTotal()).isEqualTo(Money.ZERO);
	}

	@Test
	void aBudgetCanOnlyBeDecidedOnce() {
		WorkOrder wo = waitingApproval(false);
		UUID budgetId = wo.budgets().getFirst().id();
		wo.approveBudget(budgetId, approval(CUSTOMER, DecisionChannel.APP), T0);

		assertThatThrownBy(() -> wo.approveBudget(budgetId, approval(CUSTOMER, DecisionChannel.APP), T0))
				.isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> wo.rejectBudget(budgetId, approval(CUSTOMER, DecisionChannel.APP), T0))
				.isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> wo.approveBudget(UUID.randomUUID(), approval(CUSTOMER, DecisionChannel.APP), T0))
				.isInstanceOf(WorkOrderStateException.class);
	}

	@Test
	void additionalBudgetPausesExecutionAndChainsToThePreviousBudget() {
		WorkOrder wo = waitingApproval(false);
		Budget first = wo.budgets().getFirst();
		wo.approveBudget(first.id(), approval(CUSTOMER, DecisionChannel.APP), T0);

		Budget extra = wo.proposeAdditionalBudget(List.of(
				new NewItem(ItemType.PART, pads, "Brake pads", Money.of("200"), 1),
				new NewItem(ItemType.PART, pads, "Brake pads", Money.of("200"), 1)), MECHANIC, T0.plusSeconds(400));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.WAITING_APPROVAL);
		assertThat(extra.kind()).isEqualTo(BudgetKind.ADDITIONAL);
		assertThat(extra.sequence()).isEqualTo(2);
		assertThat(extra.previousBudgetId()).isEqualTo(first.id());
		assertThat(extra.lines()).singleElement().extracting(BudgetLine::quantity).isEqualTo(2);
		assertThat(extra.total()).isEqualTo(Money.of("400"));
	}

	@Test
	void rejectingAnAdditionalBudgetResumesExecutionAndKeepsTheApprovedScope() {
		WorkOrder wo = waitingApproval(false);
		wo.approveBudget(wo.budgets().getFirst().id(), approval(CUSTOMER, DecisionChannel.APP), T0);
		Budget extra = wo.proposeAdditionalBudget(List.of(new NewItem(ItemType.SERVICE, pads, "Extra", Money.of("80"), 1)), MECHANIC, T0);

		wo.rejectBudget(extra.id(), approval(CUSTOMER, DecisionChannel.APP), T0.plusSeconds(500));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.IN_EXECUTION);
		assertThat(wo.approvedTotal()).isEqualTo(Money.of("150"));
		assertThat(wo.draftItems()).isEmpty();
	}

	@Test
	void approvingAnAdditionalBudgetWithoutPartsResumesExecutionAndSumsTheScope() {
		WorkOrder wo = waitingApproval(false);
		wo.approveBudget(wo.budgets().getFirst().id(), approval(CUSTOMER, DecisionChannel.APP), T0);
		Budget extra = wo.proposeAdditionalBudget(List.of(new NewItem(ItemType.SERVICE, pads, "Extra", Money.of("80"), 1)), MECHANIC, T0);

		wo.approveBudget(extra.id(), approval(CUSTOMER, DecisionChannel.APP), T0.plusSeconds(500));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.IN_EXECUTION);
		assertThat(wo.approvedTotal()).isEqualTo(Money.of("230"));
	}

	@Test
	void additionalBudgetNeedsExecutionAndItems() {
		assertThatThrownBy(() -> waitingApproval(false).proposeAdditionalBudget(
				List.of(new NewItem(ItemType.SERVICE, pads, "X", Money.of("1"), 1)), MECHANIC, T0)).isInstanceOf(WorkOrderStateException.class);

		WorkOrder wo = waitingApproval(false);
		wo.approveBudget(wo.budgets().getFirst().id(), approval(CUSTOMER, DecisionChannel.APP), T0);
		assertThatThrownBy(() -> wo.proposeAdditionalBudget(List.of(), MECHANIC, T0)).isInstanceOf(DomainException.class);
	}

	@Test
	void completeThenDeliverFollowsTheLifecycle() {
		WorkOrder wo = waitingApproval(false);
		wo.approveBudget(wo.budgets().getFirst().id(), approval(CUSTOMER, DecisionChannel.APP), T0);

		wo.complete(MECHANIC, T0.plusSeconds(600));
		assertThat(wo.status()).isEqualTo(WorkOrderStatus.COMPLETED);
		wo.deliver(ATTENDANT, T0.plusSeconds(700));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.DELIVERED);
		assertThat(wo.history()).extracting(StatusChange::to).containsExactly(WorkOrderStatus.RECEIVED, WorkOrderStatus.IN_DIAGNOSIS,
				WorkOrderStatus.WAITING_APPROVAL, WorkOrderStatus.IN_EXECUTION, WorkOrderStatus.COMPLETED, WorkOrderStatus.DELIVERED);
		assertThat(wo.updatedAt()).isEqualTo(T0.plusSeconds(700));
		assertThatThrownBy(() -> wo.cancel("late", ATTENDANT, T0)).isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> wo.complete(MECHANIC, T0)).isInstanceOf(WorkOrderStateException.class);
	}

	@Test
	void cannotDeliverBeforeCompletion() {
		WorkOrder wo = open();

		assertThatThrownBy(() -> wo.deliver(ATTENDANT, T0)).isInstanceOf(WorkOrderStateException.class);
		assertThatThrownBy(() -> wo.complete(MECHANIC, T0)).isInstanceOf(WorkOrderStateException.class);
	}

	@Test
	void cancellingCancelsPendingBudgetsAndKeepsTheReason() {
		WorkOrder wo = waitingApproval(false);

		wo.cancel(" customer gave up ", ATTENDANT, T0.plusSeconds(300));

		assertThat(wo.status()).isEqualTo(WorkOrderStatus.CANCELLED);
		assertThat(wo.budgets().getFirst().status()).isEqualTo(BudgetStatus.CANCELLED);
		assertThat(wo.history().getLast().note()).isEqualTo("customer gave up");
		assertThat(wo.status().terminal()).isTrue();
	}

	@Test
	void cancellingAnApprovedBudgetKeepsItApproved() {
		WorkOrder wo = waitingApproval(false);
		wo.approveBudget(wo.budgets().getFirst().id(), approval(CUSTOMER, DecisionChannel.APP), T0);

		wo.cancel(null, ATTENDANT, T0);

		assertThat(wo.budgets().getFirst().status()).isEqualTo(BudgetStatus.APPROVED);
		assertThat(wo.history().getLast().note()).isEqualTo("Cancelled");
	}

	@Test
	void cannotCancelOnceCompleted() {
		WorkOrder wo = waitingApproval(false);
		wo.approveBudget(wo.budgets().getFirst().id(), approval(CUSTOMER, DecisionChannel.APP), T0);
		wo.complete(MECHANIC, T0);

		assertThatThrownBy(() -> wo.cancel("x", ATTENDANT, T0)).isInstanceOf(WorkOrderStateException.class);
	}

	@Test
	void refreshesSnapshotsWhileActiveButNotWhenFinished() {
		WorkOrder wo = open();
		CustomerSnapshot renamed = new CustomerSnapshot(wo.customer().id(), "Ana Lima", "52998224725", "lima@example.com");
		VehicleSnapshot changed = new VehicleSnapshot(wo.vehicle().id(), "ABC1D23", "VW", "Polo", 2023);

		assertThat(wo.refreshCustomer(renamed)).isTrue();
		assertThat(wo.refreshCustomer(renamed)).isFalse();
		assertThat(wo.refreshVehicle(changed)).isTrue();
		assertThat(wo.refreshVehicle(changed)).isFalse();
		assertThat(wo.customer().name()).isEqualTo("Ana Lima");
		assertThat(wo.vehicle().model()).isEqualTo("Polo");

		wo.cancel("x", ATTENDANT, T0);
		assertThat(wo.refreshCustomer(new CustomerSnapshot(wo.customer().id(), "Other", "52998224725", "o@x.co"))).isFalse();
		assertThat(wo.refreshVehicle(new VehicleSnapshot(wo.vehicle().id(), "ABC1D23", "X", "Y", 2000))).isFalse();
	}

	@Test
	void refreshesDraftItemSnapshotsButNeverIssuedBudgets() {
		WorkOrder wo = open();

		assertThat(wo.refreshDraftItem(ItemType.SERVICE, oilChange, "Oil change plus", Money.of("170"))).isTrue();
		assertThat(wo.refreshDraftItem(ItemType.SERVICE, oilChange, "Oil change plus", Money.of("170"))).isFalse();
		assertThat(wo.refreshDraftItem(ItemType.SERVICE, UUID.randomUUID(), "X", Money.of("1"))).isFalse();
		assertThat(wo.draftItems().getFirst().unitPrice()).isEqualTo(Money.of("170"));

		wo.startDiagnosis(MECHANIC, T0);
		Budget budget = wo.completeDiagnosis(MECHANIC, T0);
		assertThat(wo.refreshDraftItem(ItemType.SERVICE, oilChange, "Oil change gold", Money.of("999"))).isFalse();
		assertThat(budget.lines().getFirst().unitPrice()).isEqualTo(Money.of("170"));
	}

	@Test
	void statusRulesAreExhaustive() {
		for (WorkOrderStatus from : WorkOrderStatus.values()) {
			assertThat(from.canTransitionTo(from)).as("%s -> itself", from).isFalse();
		}
		assertThat(WorkOrderStatus.RECEIVED.itemsEditable()).isTrue();
		assertThat(WorkOrderStatus.IN_EXECUTION.itemsEditable()).isFalse();
		assertThat(WorkOrderStatus.WAITING_PARTS.canTransitionTo(WorkOrderStatus.CANCELLED)).isTrue();
		assertThat(WorkOrderStatus.COMPLETED.canTransitionTo(WorkOrderStatus.CANCELLED)).isFalse();
		assertThat(WorkOrderStatus.DELIVERED.terminal()).isTrue();
	}
}

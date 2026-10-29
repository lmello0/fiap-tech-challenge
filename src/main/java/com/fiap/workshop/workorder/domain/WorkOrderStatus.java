package com.fiap.workshop.workorder.domain;

public enum WorkOrderStatus {
	RECEIVED, IN_DIAGNOSIS, WAITING_APPROVAL, WAITING_PARTS, IN_EXECUTION, COMPLETED, DELIVERED, CANCELLED;

	public boolean canTransitionTo(WorkOrderStatus target) {
		return switch (this) {
			case RECEIVED -> target == IN_DIAGNOSIS || target == CANCELLED;
			case IN_DIAGNOSIS -> target == WAITING_APPROVAL || target == CANCELLED;
			case WAITING_APPROVAL -> target == IN_EXECUTION || target == WAITING_PARTS || target == IN_DIAGNOSIS || target == CANCELLED;
			case WAITING_PARTS -> target == IN_EXECUTION || target == CANCELLED;
			case IN_EXECUTION -> target == WAITING_APPROVAL || target == COMPLETED || target == CANCELLED;
			case COMPLETED -> target == DELIVERED;
			case DELIVERED, CANCELLED -> false;
		};
	}

	public int queueRank() {
		return switch (this) {
			case IN_EXECUTION -> 0;
			case WAITING_PARTS -> 1;
			case WAITING_APPROVAL -> 2;
			case IN_DIAGNOSIS -> 3;
			case RECEIVED -> 4;
			case COMPLETED -> 5;
			case DELIVERED -> 6;
			case CANCELLED -> 7;
		};
	}

	public boolean inQueue() {
		return queueRank() <= RECEIVED.queueRank();
	}

	public boolean itemsEditable() {
		return this == RECEIVED || this == IN_DIAGNOSIS;
	}

	public boolean terminal() {
		return this == DELIVERED || this == CANCELLED;
	}
}

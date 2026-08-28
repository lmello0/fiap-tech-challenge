import type {
  AppointmentInfoDto,
  BlockingShortfallInfoDto,
  BudgetInfoDto,
  ClosureInfoDto,
  HistoryEntryInfoDto,
  PartInfoDto,
  PartStockInfoDto,
  PurchaseOrderInfoDto,
  RepairServiceInfoDto,
  SchedulingSettingsInfoDto,
  StockMovementInfoDto,
  StockPolicyInfoDto,
  UserInfoDto,
  VehicleInfoDto,
  VendorInfoDto,
  WorkOrderCountStatusInfoDto,
  WorkOrderInfoDto,
} from '../api/dto';
import type { WorkOrderStatus } from '../domain/enums';
import type {
  Appointment,
  Budget,
  Closure,
  Customer,
  HistoryEntry,
  Part,
  PurchaseOrder,
  RepairService,
  ReorderRule,
  SchedulingSettings,
  Shortfall,
  StockMovement,
  Vehicle,
  Vendor,
  Worker,
  WorkOrder,
} from '../domain/models';

/**
 * Wire to domain.
 *
 * The console's models are not the API's records, and the gap is deliberate in
 * both directions. Some of it is convenience — a `firstName`/`lastName` pair
 * becomes one `name`, because every screen prints it as one. Some of it is a
 * join the API does not do for us — a Part's catalog row and its derived stock
 * standing arrive from two endpoints and are merged here.
 *
 * The rule this file holds to: never invent. Where the backend has no answer,
 * the model says so with a `null` the UI is written to render, rather than a
 * zero or an empty string that reads as fact.
 */

/* --- people --------------------------------------------------------------- */

export function fullName(user: UserInfoDto): string {
  return [user.firstName, user.lastName].filter((part) => part && part.trim()).join(' ').trim();
}

function primaryPhone(user: UserInfoDto): string | null {
  if (user.phoneNumbers.length === 0) return null;
  // `isPrimary` is not guaranteed to be set on any of them — the bootstrap
  // account's sole number has it false — so the first is the honest fallback.
  return (user.phoneNumbers.find((p) => p.isPrimary) ?? user.phoneNumbers[0]).phone;
}

export function toCustomer(user: UserInfoDto): Customer {
  return {
    id: user.id,
    name: fullName(user),
    email: user.email,
    document: user.documentCode,
    documentType: user.documentType,
    phone: primaryPhone(user),
    active: user.customerActive,
    createdAt: user.createdAt,
  };
}

export function toWorker(user: UserInfoDto): Worker {
  return {
    id: user.id,
    name: fullName(user),
    email: user.email,
    phone: primaryPhone(user),
    // A `UserInfo` reaching this mapper always carries the Worker facet; the
    // fallback keeps the type honest rather than asserting non-null.
    role: user.workerRole ?? 'ATTENDANT',
    registration: user.registration,
    hiredAt: user.hireDate,
    terminatedAt: user.terminationDate,
    active: user.terminationDate === null,
  };
}

/* --- work orders ---------------------------------------------------------- */

/**
 * `WorkOrderInfo` carries bare ids for customer, vehicle and mechanic. The
 * enriched labels the board prints are resolved separately and merged in by
 * `enrich.ts`, so they are left absent here rather than guessed at.
 */
export function toWorkOrder(dto: WorkOrderInfoDto): WorkOrder {
  return { ...dto };
}

export function toBudget(dto: BudgetInfoDto): Budget {
  return {
    id: dto.id,
    workOrderId: dto.workOrderId,
    status: dto.status,
    lines: dto.lines.map((line) => ({ ...line })),
    grandTotal: dto.grandTotal,
    createdAt: dto.createdAt,
    sentAt: dto.sentAt,
    resolvedAt: dto.resolvedAt,
  };
}

/**
 * The summary's camel-cased fields, back onto the statuses they count.
 * Listed explicitly rather than derived from the enum: a rename on either side
 * should be a compile error, not a silently missing count.
 */
export function toStatusCounts(dto: WorkOrderCountStatusInfoDto): Map<WorkOrderStatus, number> {
  return new Map<WorkOrderStatus, number>([
    ['RECEIVED', dto.received],
    ['WAITING_DIAGNOSTICS', dto.waitingDiagnostics],
    ['IN_DIAGNOSTICS', dto.inDiagnostics],
    ['BUDGET_IN_DRAFT', dto.budgetInDraft],
    ['WAITING_APPROVAL', dto.waitingApproval],
    ['APPROVED', dto.approved],
    ['REFUSED', dto.refused],
    ['IN_PROGRESS', dto.inProgress],
    ['FINISHED', dto.finished],
    ['WAITING_PICKUP', dto.waitingPickup],
    ['DELIVERED', dto.delivered],
    ['CANCELLED', dto.cancelled],
  ]);
}

/* --- inventory ------------------------------------------------------------ */

/**
 * The catalog row joined to its derived stock standing.
 *
 * `stock` is optional because `/parts/stock` only reports parts the ledger has
 * ever moved. A part created but never received has no row there, and the
 * honest reading of that is zero on hand — not "unknown".
 *
 * Average cost is the exception: it stays null rather than falling to zero. A
 * part stocked by adjustment alone has never been bought, so the shop does not
 * know what it costs, and "R$ 0,00" would be a claim rather than a gap.
 */
export function toPart(part: PartInfoDto, stock: PartStockInfoDto | undefined): Part {
  return {
    id: part.id,
    sku: part.sku,
    name: part.name,
    description: part.description,
    brand: part.brand,
    unitOfMeasure: part.unitOfMeasure,
    salePrice: part.salePrice,
    averageCost: stock?.avgCostAllTime ?? null,
    quantityOnHand: stock?.onHand ?? 0,
    quantityReserved: stock?.reserved ?? 0,
    available: stock?.available ?? 0,
    stockStatus: stock?.stockStatus ?? 'NO_POLICY',
    active: part.active,
    createdAt: part.createdAt,
    updatedAt: part.updatedAt,
  };
}

export function toRepairService(dto: RepairServiceInfoDto): RepairService {
  const seconds = dto.averageSeconds ?? dto.estimatedSeconds;
  return {
    id: dto.id,
    code: dto.code,
    name: dto.name,
    description: dto.description,
    price: dto.price,
    executionMinutes: seconds === null ? null : Math.round(seconds / 60),
    // Until a service has actually been executed, its duration is the seeded
    // estimate; the UI marks the difference rather than presenting both alike.
    estimated: dto.executionCount === 0,
    executionCount: dto.executionCount,
    active: dto.active,
  };
}

export function toVendor(dto: VendorInfoDto): Vendor {
  return { id: dto.id, name: dto.name, email: dto.contactEmail, active: dto.active };
}

export function toPurchaseOrder(dto: PurchaseOrderInfoDto): PurchaseOrder {
  return {
    id: dto.id,
    code: dto.code,
    vendorId: dto.vendorId,
    status: dto.status,
    placedAt: dto.placedAt,
    expectedAt: dto.expectedAt,
    lines: dto.lines.map((line) => ({
      id: line.id,
      partId: line.partId,
      quantity: line.quantityOrdered,
      received: line.quantityReceived,
      unitCost: line.unitCost,
    })),
  };
}

export function toStockMovement(dto: StockMovementInfoDto): StockMovement {
  return { ...dto };
}

export function toReorderRule(dto: StockPolicyInfoDto): ReorderRule {
  return {
    id: dto.id,
    partId: dto.partId,
    vendorId: dto.vendorId,
    min: dto.minQuantity,
    max: dto.maxQuantity,
    enabled: dto.autoReorderEnabled,
  };
}

/**
 * A shortfall as the board reads it.
 *
 * The endpoint answers only what is short. `available` is filled from the stock
 * standing the store already holds, so the row can show both what is missing and
 * what is actually on the shelf without a second round trip per part.
 */
export function toShortfall(dto: BlockingShortfallInfoDto, available: number | undefined): Shortfall {
  return {
    partId: dto.partId,
    sku: dto.partSku,
    partName: dto.partName,
    short: dto.quantityShort,
    available: available ?? 0,
    required: (available ?? 0) + dto.quantityShort,
  };
}

/* --- scheduling ----------------------------------------------------------- */

export function toAppointment(dto: AppointmentInfoDto): Appointment {
  return { ...dto };
}

/**
 * The shop's hours. There is no configurable slot length or weekday set on the
 * wire — the backend models opening hours as a start/end time plus explicit
 * closure days — so neither is fabricated here.
 */
export function toSchedulingSettings(dto: SchedulingSettingsInfoDto): SchedulingSettings {
  return {
    openFrom: trimSeconds(dto.businessStartTime),
    openTo: trimSeconds(dto.businessEndTime),
    dropoffCapacityPerSlot: dto.dropoffSlotCapacity,
    pickupCapacityPerSlot: dto.pickupSlotCapacity,
  };
}

/** `LocalTime` serialises as `HH:mm:ss`; the shop floor reads `HH:mm`. */
function trimSeconds(time: string): string {
  return time.slice(0, 5);
}

export function toClosure(dto: ClosureInfoDto): Closure {
  return { date: dto.date, message: dto.message };
}

/* --- vehicles ------------------------------------------------------------- */

export function toVehicle(dto: VehicleInfoDto): Vehicle {
  return {
    id: dto.id,
    customerId: dto.customerId,
    licensePlate: dto.licensePlate,
    make: dto.make,
    model: dto.model,
    modelYear: dto.modelYear,
    manufactureYear: dto.manufactureYear,
    color: dto.color,
    vehicleType: dto.vehicleType,
    fuelType: dto.fuelType,
    transmissionType: dto.transmissionType,
    active: dto.active,
  };
}

/** "2019 Fiat Argo" — how the shop names a car in a sentence. */
export function vehicleLabel(vehicle: Vehicle): string {
  return [vehicle.modelYear, vehicle.make, vehicle.model].filter(Boolean).join(' ');
}

/* --- history -------------------------------------------------------------- */

/**
 * Event codes as the shop would say them aloud.
 *
 * The wire carries a stable code (`WORK_ORDER_APPROVED`), never a sentence, so
 * the sentence is the console's. Only the codes that can appear on a Work Order
 * Timeline are listed; anything else falls back to a mechanical de-casing, which
 * is worse to read but never wrong.
 */
const EVENT_SUMMARY: Record<string, string> = {
  WORK_ORDER_CREATED: 'Work order opened',
  WORK_ORDER_DIAGNOSTICS_REQUESTED: 'Diagnostics requested',
  WORK_ORDER_DIAGNOSTICS_STARTED: 'Diagnostics started',
  WORK_ORDER_APPROVED: 'Budget approved by the customer',
  WORK_ORDER_REFUSED: 'Budget refused by the customer',
  WORK_ORDER_CANCELLED: 'Work order cancelled',
  WORK_ORDER_IN_PROGRESS: 'Service started — reserved stock consumed',
  WORK_ORDER_FINISHED: 'Service finished',
  WORK_ORDER_WAITING_PICKUP: 'Marked ready for pickup',
  WORK_ORDER_DELIVERED: 'Vehicle delivered to the customer',
  WORK_ORDER_PARTS_REPLENISHED: 'Short parts replenished',
  BUDGET_DRAFTED: 'Diagnosis recorded and budget drafted',
  BUDGET_SENT: 'Budget sent to the customer — lines frozen',
  BUDGET_LINE_ADDED: 'Budget line added',
  BUDGET_LINE_REMOVED: 'Budget line removed',
  BUDGET_LINE_QUANTITY_CHANGED: 'Budget line quantity changed',
  BUDGET_LINE_STARTED: 'Work started on a budget line',
  BUDGET_LINE_FINISHED: 'Work finished on a budget line',
  PART_RESERVATION_EXPIRED: 'A part reservation expired and was released',
  PICKUP_INVITATION_REQUESTED: 'Pickup booking invitation sent',
  APPOINTMENT_CHECKED_IN: 'Appointment checked in',
  APPOINTMENT_CANCELLED: 'Appointment cancelled',
  APPOINTMENT_BOOKED: 'Appointment booked',
  APPOINTMENT_NO_SHOW: 'Recorded as a no-show',
};

export function toHistoryEntry(dto: HistoryEntryInfoDto): HistoryEntry {
  const label = dto.actorLabel;
  // `actorLabel` is a granted authority (`ROLE_CUSTOMER`), not a person's name.
  // Printing it as written puts a constant where a name belongs, so it is
  // rendered as the role it actually is until the API carries a name.
  const isRole = label !== null && label.startsWith('ROLE_');

  return {
    id: dto.id,
    aggregateType: dto.aggregateType,
    aggregateId: dto.aggregateId,
    eventType: dto.eventType,
    actorName: isRole ? roleWord(label) : label,
    actorIsSystem: dto.actorType === 'SYSTEM',
    actorIsRole: isRole,
    occurredAt: dto.occurredAt,
    summary: EVENT_SUMMARY[dto.eventType] ?? humanise(dto.eventType),
  };
}

/** `ROLE_CUSTOMER` → `a customer`. */
function roleWord(authority: string): string {
  const word = authority.slice('ROLE_'.length).toLowerCase();
  return `a ${word}`;
}

/** `PART_STOCK_LOW` becomes `Part stock low`. Last resort, never a lie. */
function humanise(code: string): string {
  const words = code.toLowerCase().replaceAll('_', ' ');
  return words.charAt(0).toUpperCase() + words.slice(1);
}

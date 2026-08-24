import type {
  AppointmentCancelReason,
  AppointmentStatus,
  AppointmentType,
  BudgetStatus,
  DocumentType,
  FuelType,
  PurchaseOrderStatus,
  RowType,
  StockMovementType,
  TransmissionType,
  UnitOfMeasure,
  VehicleType,
  WorkOrderStatus,
  WorkerRole,
} from './enums';
import type { StockStatus } from '../api/dto';

/* ---------------------------------------------------------------------------
   Read models.

   What the console works in, which is not quite what the API returns. The wire
   shapes live in `core/api/dto.ts`, one-for-one with the backend's records, and
   `core/data/mappers.ts` is the only thing that crosses between them.

   Fields marked `— enriched` are not on the wire at all. `WorkOrderInfo` and
   `AppointmentInfo` carry bare UUIDs for customer, vehicle and mechanic, so the
   labels every screen prints are resolved client-side by `core/data/enrich.ts`
   — one request per distinct id, cached for the session. They stay optional at
   the type level because enrichment can be refused: a role that may not read
   `/customers` gets the id-only rendering rather than a blank or a guess.
   --------------------------------------------------------------------------- */

export interface WorkOrder {
  id: string;
  orderCode: string;
  status: WorkOrderStatus;

  customerId: string;
  customerName?: string; // — enriched
  vehicleId: string;
  vehicleLabel?: string; // — enriched: make, model, model year
  vehiclePlate?: string; // — enriched
  assignedMechanicId: string | null;
  assignedMechanicName?: string | null; // — enriched

  customerComplaint: string;
  diagnosis: string | null;
  refusalReason: string | null;
  budgetId: string | null;

  createdAt: string;
  updatedAt: string;
  diagnosticRequestedAt: string | null;
  diagnosticStartedAt: string | null;
  diagnosticFinishedAt: string | null;
  approvedAt: string | null;
  refusedAt: string | null;
  serviceStartedAt: string | null;
  finishedAt: string | null;
  pickupReadyAt: string | null;
  deliveredAt: string | null;
}

export interface BudgetLine {
  id: string;
  type: RowType;
  description: string;
  quantity: number;
  unitPrice: number;
  lineTotal: number;
  partId: string | null;
  serviceId: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface Budget {
  id: string;
  workOrderId: string;
  status: BudgetStatus;
  lines: BudgetLine[];
  grandTotal: number;
  createdAt: string;
  sentAt: string | null;
  resolvedAt: string | null;
}

/* ---------------------------------------------------------------------------
   Shortfall — brief §7.1.

   Not exposed by the REST surface at the time of writing: it lives on
   `PartReservation` and surfaces only as `InsufficientStockException` when a
   mechanic attempts `service/start`. The board's blocked rows depend on it, so
   the shape is defined here and read through an adapter that returns an empty
   set when the endpoint is absent.
   --------------------------------------------------------------------------- */

export interface Shortfall {
  partId: string;
  sku: string;
  partName: string;
  /** How much the reservation is missing — the endpoint's own number. */
  short: number;
  /** Joined from the stock standing the store already holds, not a second call. */
  available: number;
  required: number;
}

export interface WorkOrderBlock {
  workOrderId: string;
  blocked: boolean;
  shortfalls: Shortfall[];
}

/* --------------------------------------------------------------------------- */

export interface Appointment {
  id: string;
  type: AppointmentType;
  status: AppointmentStatus;
  slotStart: string;
  slotEnd: string;
  customerId: string | null;
  customerName?: string; // — enriched
  vehicleId: string | null;
  vehicleLabel?: string; // — enriched
  vehiclePlate?: string; // — enriched
  guestName: string | null;
  guestPhone: string | null;
  guestEmail: string | null;
  guestVehicleMake: string | null;
  guestVehicleModel: string | null;
  guestVehicleYear: number | null;
  complaint: string | null;
  workOrderId: string | null;
  workOrderCode?: string | null; // — enriched
  cancelReason: AppointmentCancelReason | null;
  cancelMessage: string | null;
  rescheduledToId: string | null;
  checkedInAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Part {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  brand: string | null;
  unitOfMeasure: UnitOfMeasure;
  salePrice: number;
  /** Null until the part has actually been purchased — an adjustment carries no cost. */
  averageCost: number | null;
  quantityOnHand: number;
  quantityReserved: number;
  available: number;
  /** The ledger's own verdict against this part's reorder policy, if it has one. */
  stockStatus: StockStatus;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RepairService {
  id: string;
  code: string;
  name: string;
  description: string | null;
  price: number;
  /** Rolling average over recent executions, else the seeded estimate; null when neither exists. */
  executionMinutes: number | null;
  /** True while the duration above is still only an estimate. */
  estimated: boolean;
  executionCount: number;
  active: boolean;
}

export interface Vendor {
  id: string;
  name: string;
  email: string | null;
  active: boolean;
}

export interface PurchaseOrder {
  id: string;
  code: string;
  vendorId: string;
  vendorName?: string; // — enriched
  status: PurchaseOrderStatus;
  placedAt: string;
  expectedAt: string | null;
  /** `id` is how a receipt addresses the line it is settling. */
  lines: { id: string; partId: string; quantity: number; received: number; unitCost: number }[];
}

export interface StockMovement {
  id: string;
  partId: string;
  type: StockMovementType;
  quantity: number;
  unitCost: number | null;
  referenceId: string | null;
  referenceLabel?: string | null; // — enriched: PO code or work order code
  reason: string | null;
  occurredAt: string;
}

export interface ReorderRule {
  id: string;
  partId: string;
  vendorId: string;
  min: number;
  max: number;
  enabled: boolean;
}

export interface Customer {
  id: string;
  name: string;
  email: string;
  document: string;
  documentType: DocumentType;
  phone: string | null;
  active: boolean;
  createdAt: string;
}

export interface Vehicle {
  id: string;
  customerId: string;
  customerName?: string; // — enriched
  licensePlate: string;
  make: string;
  model: string;
  modelYear: number | null;
  manufactureYear: number | null;
  color: string | null;
  vehicleType: VehicleType;
  fuelType: FuelType;
  transmissionType: TransmissionType;
  active: boolean;
}

export interface Worker {
  id: string;
  name: string;
  email: string;
  /** Needed to edit the profile: the update command requires at least one number. */
  phone: string | null;
  role: WorkerRole;
  /** The shop's own staff number, e.g. `ARS-000001`. */
  registration: string | null;
  hiredAt: string | null;
  terminatedAt: string | null;
  /** A Worker facet is active until it is terminated. */
  active: boolean;
}

export interface HistoryEntry {
  id: string;
  aggregateType: string;
  aggregateId: string;
  /** The backend's stable code, e.g. `WORK_ORDER_APPROVED`. */
  eventType: string;
  /** A `User`, or the system itself for scheduled jobs and startup tasks. */
  actorName: string | null;
  actorIsSystem: boolean;
  /**
   * True when `actorName` is a role rather than a person. The API's `actorLabel`
   * currently carries the granted authority (`ROLE_CUSTOMER`), not a name, so the
   * timeline says which authority acted and does not pretend to know who.
   */
  actorIsRole: boolean;
  occurredAt: string;
  /** The console's sentence for `eventType`. The wire carries no prose. */
  summary: string;
}

export interface SchedulingSettings {
  /** `HH:mm`. The shop has opening hours and closure days — no weekday schedule. */
  openFrom: string;
  openTo: string;
  dropoffCapacityPerSlot: number;
  pickupCapacityPerSlot: number;
}

export interface Closure {
  date: string;
  message: string | null;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  page: number;
  size: number;
}

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

/* ---------------------------------------------------------------------------
   Read models.

   These are the ENRICHED shapes specified in the design brief (§7), not what
   `WorkOrderInfo` and `AppointmentInfo` return today. The backend currently
   hands back bare UUIDs for customer, vehicle and mechanic; the fields marked
   `— enriched` below are the ones the backend is adding in parallel. Every
   enriched field is optional at the type level so this console renders
   correctly against either contract: `core/data/enrich.ts` fills the gap.
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
  /** Present when a send attempt failed and the budget is stuck in WAITING_SEND. */
  lastSendError?: string | null;
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
  required: number;
  available: number;
  short: number;
  unitOfMeasure: UnitOfMeasure;
  /** An open purchase order that would cover it, when one exists. */
  inboundPurchaseOrderCode?: string | null;
  inboundEta?: string | null;
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
  averageCost: number;
  quantityOnHand: number;
  quantityReserved: number;
  available: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface RepairService {
  id: string;
  name: string;
  description: string | null;
  price: number;
  /** Rolling average over recent executions; falls back to the seeded estimate. */
  executionMinutes: number;
  estimated: boolean;
  active: boolean;
}

export interface Vendor {
  id: string;
  name: string;
  document: string;
  email: string | null;
  phone: string | null;
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
  lines: { partId: string; sku: string; partName: string; quantity: number; received: number; unitCost: number }[];
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
  sku?: string;
  partName?: string;
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
  vehicleCount?: number;
  createdAt: string;
}

export interface Vehicle {
  id: string;
  customerId: string;
  customerName?: string; // — enriched
  licensePlate: string;
  make: string;
  model: string;
  modelYear: number;
  manufactureYear: number;
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
  role: WorkerRole;
  hiredAt: string;
  startedAt: string | null;
  terminatedAt: string | null;
  active: boolean;
}

export interface HistoryEntry {
  id: string;
  aggregateType: string;
  aggregateId: string;
  eventType: string;
  /** A `User`, or the system itself for scheduled jobs and startup tasks. */
  actorName: string | null;
  actorIsSystem: boolean;
  customerVisible: boolean;
  occurredAt: string;
  summary: string;
}

export interface SchedulingSettings {
  openDays: number[];
  openFrom: string;
  openTo: string;
  slotMinutes: number;
  dropoffCapacityPerSlot: number;
  pickupCapacityPerSlot: number;
}

export interface Closure {
  date: string;
  message: string | null;
  cancelledAppointments: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

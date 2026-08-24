/**
 * The wire.
 *
 * Every type here is a one-for-one transcription of a Java `record` under
 * `apps/backend/**\/api/representation/`, field for field and name for name.
 * Nothing is renamed, widened or convenience-shaped on the way in — that is
 * `core/data/mappers.ts`'s job. When the backend changes a record, this file is
 * the single place the change lands, and the compiler finds the rest.
 *
 * Two things worth knowing before reading further:
 *
 *  - `BigDecimal` and `Instant` both arrive as JSON primitives (number and ISO
 *    string). They are typed as such here rather than as branded types: the
 *    console formats them at the edge and never does arithmetic that would care.
 *  - Java records serialise absent object references as `null`, never as an
 *    omitted key, so nullable fields are `T | null` rather than `T | undefined`.
 */

import type {
  AppointmentCancelReason,
  AppointmentStatus,
  AppointmentType,
  BudgetStatus,
  DocumentType,
  FuelType,
  PhoneType,
  PurchaseOrderStatus,
  RowType,
  StockMovementType,
  TransmissionType,
  UnitOfMeasure,
  VehicleType,
  WorkOrderStatus,
  WorkerRole,
} from '../domain/enums';

/** `shared/responses/PageResponse`. Note `page`, not Spring's own `number`. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** RFC 7807, as Spring renders it. `detail` is the human-readable sentence. */
export interface ProblemDetailDto {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  requestId?: string;
}

/* --- auth + user ---------------------------------------------------------- */

export interface TokenResponseDto {
  accessToken: string;
  refreshToken: string;
  /** Serialised by Jackson as ISO-8601 duration, e.g. `PT5M`. */
  expiresIn: string;
}

export interface PhoneNumberInfoDto {
  type: PhoneType;
  phone: string;
  isPrimary: boolean;
  createdAt: string;
}

/**
 * One `User`, carrying both facets. `customer` and `worker` are independent
 * booleans — the bootstrap manager is both at once, which is why neither may be
 * inferred from the other (CONTEXT.md, dual User facets).
 */
export interface UserInfoDto {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  documentType: DocumentType;
  documentCode: string;
  phoneNumbers: PhoneNumberInfoDto[];
  emailVerified: boolean;
  customer: boolean;
  customerActive: boolean;
  worker: boolean;
  workerRole: WorkerRole | null;
  registration: string | null;
  hireDate: string | null;
  terminationDate: string | null;
  createdAt: string;
  updatedAt: string;
}

/* --- work orders ---------------------------------------------------------- */

export interface WorkOrderInfoDto {
  id: string;
  orderCode: string;
  status: WorkOrderStatus;
  customerId: string;
  vehicleId: string;
  assignedMechanicId: string | null;
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

/**
 * One count per lifecycle status. Note the fields are camel-cased status names,
 * not a map keyed by the enum, so the console translates rather than indexes.
 */
export interface WorkOrderCountStatusInfoDto {
  received: number;
  waitingDiagnostics: number;
  inDiagnostics: number;
  budgetInDraft: number;
  waitingApproval: number;
  approved: number;
  refused: number;
  inProgress: number;
  finished: number;
  waitingPickup: number;
  delivered: number;
}

export interface BudgetLineInfoDto {
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

export interface BudgetInfoDto {
  id: string;
  workOrderId: string;
  status: BudgetStatus;
  lines: BudgetLineInfoDto[];
  grandTotal: number;
  createdAt: string;
  sentAt: string | null;
  resolvedAt: string | null;
}

/**
 * `CustomerWorkOrderView` — deliberately narrow.
 *
 * The customer facet is never served `WorkOrderInfoDto`. No assigned mechanic,
 * no diagnosis notes, no internal timestamps: the code, where the job stands,
 * and the budget they are being asked to decide on. The customer console must
 * not reconstruct what the API withholds on purpose.
 */
export interface CustomerWorkOrderViewDto {
  id: string;
  orderCode: string;
  status: WorkOrderStatus;
  budget: BudgetInfoDto | null;
}

/* --- inventory ------------------------------------------------------------ */

/** Catalog only. Stock lives in `PartStockInfoDto`, deliberately (ADR 0012). */
export interface PartInfoDto {
  id: string;
  sku: string;
  name: string;
  description: string | null;
  brand: string | null;
  unitOfMeasure: UnitOfMeasure;
  salePrice: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export type StockStatus = 'OUT' | 'LOW' | 'OK' | 'NO_POLICY';

/** The derived ledger standing for one part, keyed by `partId`, not `id`. */
export interface PartStockInfoDto {
  partId: string;
  onHand: number;
  reserved: number;
  available: number;
  stockStatus: StockStatus;
  avgCost30d: number | null;
  avgCost90d: number | null;
  avgCost365d: number | null;
  avgCostAllTime: number | null;
}

export interface RepairServiceInfoDto {
  id: string;
  code: string;
  name: string;
  description: string | null;
  price: number;
  estimatedSeconds: number | null;
  /** The measured rolling average once executions exist, else `estimatedSeconds`. */
  averageSeconds: number | null;
  executionCount: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VendorInfoDto {
  id: string;
  name: string;
  contactEmail: string | null;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface PurchaseOrderLineInfoDto {
  id: string;
  partId: string;
  quantityOrdered: number;
  quantityReceived: number;
  unitCost: number;
}

export interface PurchaseOrderInfoDto {
  id: string;
  code: string;
  vendorId: string;
  status: PurchaseOrderStatus;
  vendorOrderRef: string | null;
  placedAt: string;
  expectedAt: string | null;
  receivedAt: string | null;
  updatedAt: string;
  lines: PurchaseOrderLineInfoDto[];
}

export interface StockMovementInfoDto {
  id: string;
  partId: string;
  type: StockMovementType;
  quantity: number;
  unitCost: number | null;
  referenceId: string | null;
  reason: string | null;
  occurredAt: string;
}

export interface StockPolicyInfoDto {
  id: string;
  partId: string;
  minQuantity: number;
  maxQuantity: number;
  vendorId: string;
  autoReorderEnabled: boolean;
  createdAt: string;
  updatedAt: string;
}

/** One part still short on a work order's reservations. Brief §7.1's endpoint. */
export interface BlockingShortfallInfoDto {
  partId: string;
  partSku: string;
  partName: string;
  quantityShort: number;
}

/* --- scheduling ----------------------------------------------------------- */

export interface AppointmentInfoDto {
  id: string;
  type: AppointmentType;
  status: AppointmentStatus;
  slotStart: string;
  slotEnd: string;
  customerId: string | null;
  vehicleId: string | null;
  guestName: string | null;
  guestPhone: string | null;
  guestEmail: string | null;
  guestVehicleMake: string | null;
  guestVehicleModel: string | null;
  guestVehicleYear: number | null;
  complaint: string | null;
  workOrderId: string | null;
  cancelReason: AppointmentCancelReason | null;
  cancelMessage: string | null;
  rescheduledToId: string | null;
  checkedInAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/** `LocalTime` fields arrive as `HH:mm:ss`. */
export interface SchedulingSettingsInfoDto {
  businessStartTime: string;
  businessEndTime: string;
  dropoffSlotCapacity: number;
  pickupSlotCapacity: number;
}

export interface ClosureInfoDto {
  id: string;
  date: string;
  message: string | null;
  createdAt: string;
}

/* --- vehicles ------------------------------------------------------------- */

export interface VehicleInfoDto {
  id: string;
  customerId: string;
  vehicleType: VehicleType;
  licensePlate: string;
  make: string;
  model: string;
  color: string | null;
  modelYear: number | null;
  manufactureYear: number | null;
  version: string | null;
  fuelType: FuelType;
  transmissionType: TransmissionType;
  active: boolean;
  deactivatedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

/* --- history -------------------------------------------------------------- */

export type ActorType = 'USER' | 'SYSTEM';

/**
 * One Timeline row, without its Snapshot. There is no `summary` field and no
 * `customerVisible` flag on the wire — both are the console's own reading of
 * `eventType`, applied in `mappers.ts`.
 */
export interface HistoryEntryInfoDto {
  id: string;
  aggregateType: string;
  aggregateId: string;
  entityType: string | null;
  entityId: string | null;
  eventType: string;
  occurredAt: string;
  actorType: ActorType;
  actorId: string | null;
  actorLabel: string | null;
}

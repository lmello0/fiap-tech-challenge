import { Injectable, inject } from '@angular/core';
import { ApiClient, type QueryValue } from './api-client';
import type {
  AppointmentInfoDto,
  BlockingShortfallInfoDto,
  BudgetInfoDto,
  ClosureInfoDto,
  HistoryEntryInfoDto,
  PageResponse,
  PartInfoDto,
  PartStockInfoDto,
  PurchaseOrderInfoDto,
  RepairServiceInfoDto,
  SchedulingSettingsInfoDto,
  StockMovementInfoDto,
  StockPolicyInfoDto,
  TokenResponseDto,
  UserInfoDto,
  VehicleInfoDto,
  VendorInfoDto,
  WorkOrderCountStatusInfoDto,
  WorkOrderInfoDto,
} from './dto';
import type {
  AppointmentType,
  DocumentType,
  FuelType,
  PhoneType,
  RowType,
  TransmissionType,
  UnitOfMeasure,
  VehicleType,
  WorkOrderStatus,
  WorkerRole,
} from '../domain/enums';

/** `CreateUserCommand` / `UpdateUserProfileCommand`. At least one phone is required. */
export interface PhoneCommand {
  type: PhoneType;
  phone: string;
  isPrimary: boolean;
}

export interface CustomerCommand {
  email: string;
  firstName: string;
  lastName: string;
  documentType: DocumentType;
  documentCode: string;
  phoneNumbers: PhoneCommand[];
}

/** A profile update cannot change the email or the document. */
export interface CustomerProfileCommand {
  firstName: string;
  lastName: string;
  phoneNumbers: PhoneCommand[];
}

export interface VehicleCommand {
  customerId?: string;
  vehicleType: VehicleType;
  licensePlate: string;
  make: string;
  model: string;
  color: string;
  modelYear: number;
  manufactureYear?: number | null;
  version?: string | null;
  fuelType: FuelType;
  transmissionType: TransmissionType;
}

export interface PartCommand {
  sku?: string;
  name: string;
  description?: string | null;
  brand?: string | null;
  unitOfMeasure: UnitOfMeasure;
  salePrice: number;
}

/** Mirrors `WorkOrderFilterQuery`. Every field is ANDed by the backend. */
export interface WorkOrderFilter {
  status?: readonly WorkOrderStatus[];
  code?: string;
  customerName?: string;
  vehiclePlate?: string;
  vehicleMake?: string;
  vehicleModel?: string;
  mechanicName?: string;
}

export interface VendorCommand {
  name: string;
  contactEmail?: string | null;
}

export interface PlaceOrderCommand {
  vendorId: string;
  lines: { partId: string; quantity: number }[];
}

/** A receipt names the line, what arrived, and what it cost — cost moves the average. */
export interface ReceiveOrderCommand {
  lines: { lineId: string; quantityReceived: number; unitCost: number }[];
}

export interface StockPolicyCommand {
  partId?: string;
  minQuantity: number;
  maxQuantity?: number | null;
  vendorId?: string | null;
  autoReorderEnabled: boolean;
}

export interface ClosureCommand {
  date: string;
  message?: string | null;
}

/** `LocalTime` on the wire: `HH:mm` is accepted and `HH:mm:ss` is returned. */
export interface SchedulingSettingsCommand {
  businessStartTime: string;
  businessEndTime: string;
  dropoffSlotCapacity?: number;
  pickupSlotCapacity?: number;
}

export interface WorkerCommand {
  user: CustomerCommand;
  role: WorkerRole;
  hireDate: string;
  startDate: string;
}

export interface DropoffOnBehalfCommand {
  customerId?: string | null;
  vehicleId?: string | null;
  guestName?: string | null;
  guestPhone?: string | null;
  guestEmail?: string | null;
  guestVehicleMake?: string | null;
  guestVehicleModel?: string | null;
  guestVehicleYear?: number | null;
  complaint: string;
  slotStart: string;
}

export interface RepairServiceCommand {
  code?: string;
  name: string;
  description?: string | null;
  price: number;
  estimatedSeconds: number;
}

/**
 * Every endpoint this console uses, and no others.
 *
 * One method per operation, named for the operation rather than for the verb
 * and path, so a reader of the store sees the shop's vocabulary — `startService`,
 * `sendBudget` — instead of HTTP. Roles are noted where the backend's
 * `@PreAuthorize` is narrower than "any staff", because that is the rule the UI
 * mirrors when it decides whether to offer an action at all.
 */
@Injectable({ providedIn: 'root' })
export class ShopApi {
  private readonly api = inject(ApiClient);

  /**
   * Lists are paginated. The console's sections are shop-sized — a few hundred
   * rows at the outside — and every one of them filters and sorts client-side
   * over the whole set, so pages are pulled at a size that covers the shop in
   * one request rather than wired to a pager that would fight the filters.
   */
  private readonly pageSize = 200;

  private page<T>(path: string, query: Record<string, QueryValue> = {}): Promise<PageResponse<T>> {
    return this.api.get<PageResponse<T>>(path, { page: 0, size: this.pageSize, ...query });
  }

  /* --- auth ------------------------------------------------------------- */

  login(email: string, rawPassword: string): Promise<TokenResponseDto> {
    return this.api.post<TokenResponseDto>('/auth/login', { email, rawPassword });
  }

  logout(refreshToken: string): Promise<void> {
    return this.api.post<void>('/auth/logout', { refreshToken });
  }

  refresh(refreshToken: string): Promise<TokenResponseDto> {
    return this.api.post<TokenResponseDto>('/auth/refresh-token', { refreshToken });
  }

  me(): Promise<UserInfoDto> {
    return this.api.get<UserInfoDto>('/users/me');
  }

  /** Any staff principal may resolve a user by id — unlike `/workers/{id}`, which is MANAGER-only. */
  user(id: string): Promise<UserInfoDto> {
    return this.api.get<UserInfoDto>(`/users/${id}`);
  }

  /* --- work orders ------------------------------------------------------ */

  /**
   * The board's query, run by the database rather than over a loaded page.
   *
   * Every field on `WorkOrderFilterQuery` is ANDed, so a caller narrows by
   * combining fields — one free-text term cannot fan out across several of them.
   * Absent fields are dropped rather than sent blank (see `toParams`).
   */
  workOrders(filter: WorkOrderFilter = {}): Promise<PageResponse<WorkOrderInfoDto>> {
    return this.page<WorkOrderInfoDto>('/work-orders', {
      status: filter.status,
      code: filter.code,
      customerName: filter.customerName,
      vehiclePlate: filter.vehiclePlate,
      vehicleMake: filter.vehicleMake,
      vehicleModel: filter.vehicleModel,
      mechanicName: filter.mechanicName,
      sort: 'createdAt,desc',
    });
  }

  workOrder(id: string): Promise<WorkOrderInfoDto> {
    return this.api.get<WorkOrderInfoDto>(`/work-orders/${id}`);
  }

  /**
   * Counts per status, computed by the database rather than from a page.
   * Unbounded unless a window is asked for; the board wants the shop as it stands.
   */
  workOrderSummary(start?: string, end?: string): Promise<WorkOrderCountStatusInfoDto> {
    return this.api.get<WorkOrderCountStatusInfoDto>('/work-orders/summary', { start, end });
  }

  workOrderHistory(id: string): Promise<PageResponse<HistoryEntryInfoDto>> {
    return this.page<HistoryEntryInfoDto>(`/work-orders/${id}/history`, { sort: 'occurredAt,desc' });
  }

  createWorkOrder(customerId: string, vehicleId: string, complaint: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>('/work-orders', { customerId, vehicleId, complaint });
  }

  /** ATTENDANT or MANAGER. */
  requestDiagnostics(id: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/diagnostics/request`);
  }

  /** MECHANIC or MANAGER. Assigns the mechanic as it starts. */
  startDiagnostics(id: string, mechanicId: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/diagnostics/start`, { mechanicId });
  }

  /**
   * MECHANIC or MANAGER. Records the diagnosis and drafts the Budget atomically,
   * seeded with `lines` — which the backend requires to be non-empty.
   */
  finishDiagnostics(
    id: string,
    diagnosis: string,
    lines: readonly { type: RowType; quantity: number; partId?: string; serviceId?: string }[],
  ): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/diagnostics/finish`, { diagnosis, lines });
  }

  /** MECHANIC or MANAGER. Consumes the reserved stock; refused with 409 on a shortfall. */
  startService(id: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/service/start`);
  }

  finishService(id: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/service/finish`);
  }

  startLine(id: string, lineId: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/lines/${lineId}/start`);
  }

  finishLine(id: string, lineId: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/lines/${lineId}/finish`);
  }

  /** ATTENDANT or MANAGER. */
  pickupReady(id: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/pickup-ready`);
  }

  /** ATTENDANT or MANAGER. */
  recordDelivery(id: string): Promise<WorkOrderInfoDto> {
    return this.api.post<WorkOrderInfoDto>(`/work-orders/${id}/delivery`);
  }

  /* --- budgets ---------------------------------------------------------- */

  budget(id: string): Promise<BudgetInfoDto> {
    return this.api.get<BudgetInfoDto>(`/budgets/${id}`);
  }

  /** MECHANIC or MANAGER, draft only. Reserves stock as a side effect. */
  addBudgetLine(
    budgetId: string,
    line: { type: RowType; quantity: number; partId?: string; serviceId?: string },
  ): Promise<BudgetInfoDto> {
    return this.api.post<BudgetInfoDto>(`/budgets/${budgetId}/lines`, line);
  }

  removeBudgetLine(budgetId: string, lineId: string): Promise<BudgetInfoDto> {
    return this.api.delete<BudgetInfoDto>(`/budgets/${budgetId}/lines/${lineId}`);
  }

  changeBudgetLineQuantity(budgetId: string, lineId: string, quantity: number): Promise<BudgetInfoDto> {
    return this.api.patch<BudgetInfoDto>(`/budgets/${budgetId}/lines/${lineId}/quantity`, { quantity });
  }

  /** ATTENDANT or MANAGER. Freezes the budget's lines permanently. */
  sendBudget(budgetId: string): Promise<BudgetInfoDto> {
    return this.api.post<BudgetInfoDto>(`/budgets/${budgetId}/send`);
  }

  resendBudget(budgetId: string): Promise<BudgetInfoDto> {
    return this.api.post<BudgetInfoDto>(`/budgets/${budgetId}/resend`);
  }

  /* --- inventory -------------------------------------------------------- */

  parts(): Promise<PageResponse<PartInfoDto>> {
    return this.page<PartInfoDto>('/parts');
  }

  /** STOCKIST or MANAGER. The SKU is set at creation and never changes. */
  createPart(command: PartCommand): Promise<PartInfoDto> {
    return this.api.post<PartInfoDto>('/parts', command);
  }

  updatePart(id: string, command: PartCommand): Promise<PartInfoDto> {
    return this.api.patch<PartInfoDto>(`/parts/${id}`, command);
  }

  deactivatePart(id: string): Promise<void> {
    return this.api.delete<void>(`/parts/${id}`);
  }

  /** The whole derived stock standing in one request — joined to the catalog by `partId`. */
  partStock(): Promise<PageResponse<PartStockInfoDto>> {
    return this.page<PartStockInfoDto>('/parts/stock');
  }

  stockMovements(partId: string): Promise<PageResponse<StockMovementInfoDto>> {
    return this.page<StockMovementInfoDto>(`/parts/${partId}/stock/movements`, { sort: 'occurredAt,desc' });
  }

  /** STOCKIST or MANAGER. Signed: positive adds, negative removes, zero is rejected. */
  adjustStock(partId: string, quantity: number, reason: string): Promise<void> {
    return this.api.post<void>(`/parts/${partId}/stock/adjustments`, { quantity, reason });
  }

  services(): Promise<PageResponse<RepairServiceInfoDto>> {
    return this.page<RepairServiceInfoDto>('/services');
  }

  createService(command: RepairServiceCommand): Promise<RepairServiceInfoDto> {
    return this.api.post<RepairServiceInfoDto>('/services', command);
  }

  updateService(id: string, command: RepairServiceCommand): Promise<RepairServiceInfoDto> {
    return this.api.patch<RepairServiceInfoDto>(`/services/${id}`, command);
  }

  deactivateService(id: string): Promise<void> {
    return this.api.delete<void>(`/services/${id}`);
  }

  vendors(): Promise<PageResponse<VendorInfoDto>> {
    return this.page<VendorInfoDto>('/vendors');
  }

  createVendor(command: VendorCommand): Promise<VendorInfoDto> {
    return this.api.post<VendorInfoDto>('/vendors', command);
  }

  updateVendor(id: string, command: VendorCommand): Promise<VendorInfoDto> {
    return this.api.patch<VendorInfoDto>(`/vendors/${id}`, command);
  }

  deactivateVendor(id: string): Promise<void> {
    return this.api.delete<void>(`/vendors/${id}`);
  }

  purchaseOrders(): Promise<PageResponse<PurchaseOrderInfoDto>> {
    return this.page<PurchaseOrderInfoDto>('/purchase-orders');
  }

  /** Placing forwards the order to the vendor's own system as a side effect. */
  placePurchaseOrder(command: PlaceOrderCommand): Promise<PurchaseOrderInfoDto> {
    return this.api.post<PurchaseOrderInfoDto>('/purchase-orders', command);
  }

  /**
   * Receiving raises on-hand stock and moves the part's moving-average cost, and
   * settles the order at PARTIALLY_RECEIVED or RECEIVED depending on the lines.
   */
  receivePurchaseOrder(id: string, command: ReceiveOrderCommand): Promise<PurchaseOrderInfoDto> {
    return this.api.post<PurchaseOrderInfoDto>(`/purchase-orders/${id}/receipts`, command);
  }

  cancelPurchaseOrder(id: string): Promise<PurchaseOrderInfoDto> {
    return this.api.post<PurchaseOrderInfoDto>(`/purchase-orders/${id}/cancellation`);
  }

  stockPolicies(): Promise<PageResponse<StockPolicyInfoDto>> {
    return this.page<StockPolicyInfoDto>('/stock-policies');
  }

  /** Evaluated immediately: a part already below the minimum signals a reorder at once. */
  createStockPolicy(command: StockPolicyCommand): Promise<StockPolicyInfoDto> {
    return this.api.post<StockPolicyInfoDto>('/stock-policies', command);
  }

  updateStockPolicy(id: string, command: StockPolicyCommand): Promise<StockPolicyInfoDto> {
    return this.api.patch<StockPolicyInfoDto>(`/stock-policies/${id}`, command);
  }

  /** A genuine delete — a threshold is a rule, not a record with a history. */
  deleteStockPolicy(id: string): Promise<void> {
    return this.api.delete<void>(`/stock-policies/${id}`);
  }

  /**
   * Brief §7.1's endpoint, now real: the parts still short on a work order's
   * reservations. MECHANIC, STOCKIST or MANAGER — an ATTENDANT is answered 403,
   * which the store reads as "unknown", never as "clear".
   */
  blockingShortfalls(workOrderId: string): Promise<BlockingShortfallInfoDto[]> {
    return this.api.get<BlockingShortfallInfoDto[]>(`/work-orders/${workOrderId}/blocking-shortfalls`);
  }

  /* --- records ---------------------------------------------------------- */

  customers(): Promise<PageResponse<UserInfoDto>> {
    return this.page<UserInfoDto>('/customers');
  }

  customer(id: string): Promise<UserInfoDto> {
    return this.api.get<UserInfoDto>(`/customers/${id}`);
  }

  /**
   * ATTENDANT or MANAGER. Creates the Customer facet only — no credential is
   * set, so the person exists as a record and reaches their own account through
   * the password-reset flow.
   */
  createCustomer(command: CustomerCommand): Promise<UserInfoDto> {
    return this.api.post<UserInfoDto>('/customers', command);
  }

  updateCustomer(id: string, command: CustomerProfileCommand): Promise<UserInfoDto> {
    return this.api.patch<UserInfoDto>(`/customers/${id}`, command);
  }

  /** Deactivates; the record and its history remain. */
  deactivateCustomer(id: string): Promise<void> {
    return this.api.delete<void>(`/customers/${id}`);
  }

  reactivateCustomer(id: string): Promise<void> {
    return this.api.post<void>(`/customers/${id}/reactivate`);
  }

  vehicles(): Promise<PageResponse<VehicleInfoDto>> {
    return this.page<VehicleInfoDto>('/vehicles');
  }

  vehicle(id: string): Promise<VehicleInfoDto> {
    return this.api.get<VehicleInfoDto>(`/vehicles/${id}`);
  }

  /** Staff must name the owning customer explicitly; a CUSTOMER caller cannot. */
  createVehicle(command: VehicleCommand): Promise<VehicleInfoDto> {
    return this.api.post<VehicleInfoDto>('/vehicles', command);
  }

  updateVehicle(id: string, command: VehicleCommand): Promise<VehicleInfoDto> {
    return this.api.patch<VehicleInfoDto>(`/vehicles/${id}`, command);
  }

  deactivateVehicle(id: string): Promise<void> {
    return this.api.delete<void>(`/vehicles/${id}`);
  }

  /** MANAGER only. Other roles resolve a mechanic through `user(id)` instead. */
  workers(): Promise<PageResponse<UserInfoDto>> {
    return this.page<UserInfoDto>('/workers');
  }

  /**
   * MANAGER only. Unlike a customer, a worker is given a password at creation —
   * they must be able to sign in to the console on their first shift.
   */
  registerWorker(worker: WorkerCommand, rawPassword: string): Promise<UserInfoDto> {
    return this.api.post<UserInfoDto>('/auth/register/worker', { worker, rawPassword });
  }

  updateWorker(id: string, command: CustomerProfileCommand): Promise<UserInfoDto> {
    return this.api.patch<UserInfoDto>(`/workers/${id}`, command);
  }

  /** Terminates the Worker facet, dated today. Any Customer facet is untouched. */
  terminateWorker(id: string): Promise<void> {
    return this.api.delete<void>(`/workers/${id}`);
  }

  /* --- scheduling ------------------------------------------------------- */

  appointments(): Promise<PageResponse<AppointmentInfoDto>> {
    return this.page<AppointmentInfoDto>('/appointments', { sort: 'slotStart,asc' });
  }

  schedulingSettings(): Promise<SchedulingSettingsInfoDto> {
    return this.api.get<SchedulingSettingsInfoDto>('/scheduling/settings');
  }

  /** MANAGER only. */
  updateSchedulingSettings(command: SchedulingSettingsCommand): Promise<SchedulingSettingsInfoDto> {
    return this.api.put<SchedulingSettingsInfoDto>('/scheduling/settings', command);
  }

  /** A bare array, not a page — the one list endpoint that is not paginated. */
  closures(): Promise<ClosureInfoDto[]> {
    return this.api.get<ClosureInfoDto[]>('/scheduling/closures');
  }

  /** MANAGER only. Closing a date cancels what was already booked into it. */
  createClosure(command: ClosureCommand): Promise<ClosureInfoDto> {
    return this.api.post<ClosureInfoDto>('/scheduling/closures', command);
  }

  deleteClosure(date: string): Promise<void> {
    return this.api.delete<void>(`/scheduling/closures/${date}`);
  }

  /** The slots still open on a date, as instants. Public — no role required. */
  availability(type: AppointmentType, date: string): Promise<string[]> {
    return this.api.get<string[]>('/appointments/availability', { type, date });
  }

  /** Staff booking a drop-off for a customer, or for a guest who is not one yet. */
  bookDropoffOnBehalf(command: DropoffOnBehalfCommand): Promise<AppointmentInfoDto> {
    return this.api.post<AppointmentInfoDto>('/appointments/dropoff/on-behalf', command);
  }

  reschedule(appointmentId: string, newSlotStart: string): Promise<AppointmentInfoDto> {
    return this.api.post<AppointmentInfoDto>(`/appointments/${appointmentId}/reschedule`, {
      newSlotStart,
    });
  }

  checkIn(appointmentId: string): Promise<AppointmentInfoDto> {
    return this.api.post<AppointmentInfoDto>(`/appointments/${appointmentId}/check-in`);
  }

  cancelAppointment(appointmentId: string, message: string | null): Promise<AppointmentInfoDto> {
    return this.api.post<AppointmentInfoDto>(`/appointments/${appointmentId}/cancel`, { message });
  }
}

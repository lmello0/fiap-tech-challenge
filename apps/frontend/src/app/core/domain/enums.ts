/**
 * Backend enums, mirrored. Names match the Java enums exactly — the domain's
 * own vocabulary is the UI's vocabulary (CONTEXT.md), so nothing is renamed on
 * the way in. Display strings live beside each enum rather than in templates.
 */

export type WorkerRole = 'MECHANIC' | 'ATTENDANT' | 'MANAGER' | 'STOCKIST';

export const WORKER_ROLE_LABEL: Record<WorkerRole, string> = {
  MECHANIC: 'Mechanic',
  ATTENDANT: 'Attendant',
  MANAGER: 'Manager',
  STOCKIST: 'Stockist',
};

export type WorkOrderStatus =
  | 'RECEIVED'
  | 'WAITING_DIAGNOSTICS'
  | 'IN_DIAGNOSTICS'
  | 'BUDGET_IN_DRAFT'
  | 'WAITING_APPROVAL'
  | 'APPROVED'
  | 'REFUSED'
  | 'IN_PROGRESS'
  | 'FINISHED'
  | 'WAITING_PICKUP'
  | 'DELIVERED';

export type BudgetStatus = 'DRAFT' | 'WAITING_SEND' | 'SENT' | 'APPROVED' | 'REFUSED';

export const BUDGET_STATUS_LABEL: Record<BudgetStatus, string> = {
  DRAFT: 'Draft',
  WAITING_SEND: 'Waiting send',
  SENT: 'Sent',
  APPROVED: 'Approved',
  REFUSED: 'Refused',
};

export type RowType = 'SERVICE' | 'PART';

export type AppointmentType = 'DROPOFF' | 'PICKUP';
export type AppointmentStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'NO_SHOW';
export type AppointmentCancelReason =
  | 'CUSTOMER_REQUESTED'
  | 'STAFF_REQUESTED'
  | 'RESCHEDULED'
  | 'MANAGER_CLOSURE';

export const APPOINTMENT_STATUS_LABEL: Record<AppointmentStatus, string> = {
  SCHEDULED: 'Scheduled',
  COMPLETED: 'Checked in',
  CANCELLED: 'Cancelled',
  NO_SHOW: 'No-show',
};

export const APPOINTMENT_CANCEL_REASON_LABEL: Record<AppointmentCancelReason, string> = {
  CUSTOMER_REQUESTED: 'Customer requested',
  STAFF_REQUESTED: 'Staff requested',
  RESCHEDULED: 'Rescheduled',
  MANAGER_CLOSURE: 'Date closed by manager',
};

export type PurchaseOrderStatus = 'PLACED' | 'PARTIALLY_RECEIVED' | 'RECEIVED' | 'CANCELLED';

export const PURCHASE_ORDER_STATUS_LABEL: Record<PurchaseOrderStatus, string> = {
  PLACED: 'Placed',
  PARTIALLY_RECEIVED: 'Partially received',
  RECEIVED: 'Received',
  CANCELLED: 'Cancelled',
};

export type StockMovementType = 'PURCHASE' | 'CONSUMPTION' | 'ADJUSTMENT';

export const STOCK_MOVEMENT_LABEL: Record<StockMovementType, string> = {
  PURCHASE: 'Purchase',
  CONSUMPTION: 'Consumption',
  ADJUSTMENT: 'Adjustment',
};

export type ReservationStatus = 'HELD' | 'CONSUMED' | 'RELEASED' | 'EXPIRED';

export type UnitOfMeasure = 'UNIT' | 'LITER' | 'KILOGRAM' | 'METER' | 'SET';

export const UOM_ABBR: Record<UnitOfMeasure, string> = {
  UNIT: 'un',
  LITER: 'L',
  KILOGRAM: 'kg',
  METER: 'm',
  SET: 'set',
};

export type DocumentType = 'CPF' | 'CNPJ';
export type PhoneType = 'MOBILE' | 'COMMERCIAL' | 'HOME' | 'OTHER';

export type VehicleType = 'CAR' | 'MOTORCYCLE' | 'TRUCK' | 'VAN' | 'SUV' | 'BUS' | 'OTHER';
export type FuelType =
  | 'GASOLINE'
  | 'ETHANOL'
  | 'FLEX'
  | 'DIESEL'
  | 'ELECTRIC'
  | 'GNV'
  | 'HYBRID'
  | 'OTHER';
export type TransmissionType = 'MANUAL' | 'AUTOMATIC' | 'CVT' | 'AUTOMATED_MANUAL';

export const TRANSMISSION_LABEL: Record<TransmissionType, string> = {
  MANUAL: 'Manual',
  AUTOMATIC: 'Automatic',
  CVT: 'CVT',
  AUTOMATED_MANUAL: 'Automated manual',
};

export const VEHICLE_TYPE_LABEL: Record<VehicleType, string> = {
  CAR: 'Car',
  MOTORCYCLE: 'Motorcycle',
  TRUCK: 'Truck',
  VAN: 'Van',
  SUV: 'SUV',
  BUS: 'Bus',
  OTHER: 'Other',
};

export const FUEL_TYPE_LABEL: Record<FuelType, string> = {
  GASOLINE: 'Gasoline',
  ETHANOL: 'Ethanol',
  FLEX: 'Flex',
  DIESEL: 'Diesel',
  ELECTRIC: 'Electric',
  GNV: 'GNV',
  HYBRID: 'Hybrid',
  OTHER: 'Other',
};

export const PHONE_TYPE_LABEL: Record<PhoneType, string> = {
  MOBILE: 'Mobile',
  COMMERCIAL: 'Work',
  HOME: 'Home',
  OTHER: 'Other',
};

export const DOCUMENT_TYPE_LABEL: Record<DocumentType, string> = {
  CPF: 'CPF',
  CNPJ: 'CNPJ',
};

/**
 * SYNTHETIC SHOP DATA — illustrative only.
 *
 * Every customer, vehicle, plate, document, price and work order below is
 * invented to exercise the console at realistic density. None of it is real
 * shop data, and none of it should ship to production. Replace wholesale with
 * a live API once the backend is reachable; the shapes match
 * `core/domain/models.ts` exactly, including the enriched fields the backend is
 * adding (design brief §7).
 *
 * Volumes follow the brief's stated ranges: ~20 live work orders, ~1.3 vehicles
 * per customer, a few hundred catalogue lines represented by a working sample.
 *
 * Data is deterministic — no randomness — so screenshots and tests are stable.
 */

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
  StockMovement,
  Vehicle,
  Vendor,
  Worker,
  WorkOrder,
  WorkOrderBlock,
} from '../domain/models';

/** Fixed "today" so the demo reads consistently. */
export const DEMO_NOW = new Date('2026-08-22T14:20:00Z');

const day = (offsetDays: number, hh = 9, mm = 0): string => {
  const d = new Date(DEMO_NOW);
  d.setUTCDate(d.getUTCDate() + offsetDays);
  d.setUTCHours(hh, mm, 0, 0);
  return d.toISOString();
};

/* --------------------------------------------------------------------------
   Workers
   -------------------------------------------------------------------------- */

export const DEMO_WORKERS: Worker[] = [
  {
    id: 'w-001',
    name: 'Rafael Nogueira',
    email: 'rafael.nogueira@oficina.example',
    phone: '11987650000',
    role: 'ATTENDANT',
    hiredAt: '2023-02-06',
    registration: null,
    terminatedAt: null,
    active: true,
  },
  {
    id: 'w-002',
    name: 'Beatriz Salgado',
    email: 'beatriz.salgado@oficina.example',
    phone: '11987650000',
    role: 'MECHANIC',
    hiredAt: '2021-09-01',
    registration: null,
    terminatedAt: null,
    active: true,
  },
  {
    id: 'w-003',
    name: 'Cláudia Ferraz',
    email: 'claudia.ferraz@oficina.example',
    phone: '11987650000',
    role: 'MANAGER',
    hiredAt: '2019-04-22',
    registration: null,
    terminatedAt: null,
    active: true,
  },
  {
    id: 'w-004',
    name: 'Dario Pinheiro',
    email: 'dario.pinheiro@oficina.example',
    phone: '11987650000',
    role: 'STOCKIST',
    hiredAt: '2022-11-14',
    registration: null,
    terminatedAt: null,
    active: true,
  },
  {
    id: 'w-005',
    name: 'Emerson Vidal',
    email: 'emerson.vidal@oficina.example',
    phone: '11987650000',
    role: 'MECHANIC',
    hiredAt: '2020-06-08',
    registration: null,
    terminatedAt: null,
    active: true,
  },
  {
    id: 'w-006',
    name: 'Helena Braga',
    email: 'helena.braga@oficina.example',
    phone: '11987650000',
    role: 'MECHANIC',
    hiredAt: '2024-03-04',
    registration: null,
    terminatedAt: null,
    active: true,
  },
  {
    id: 'w-007',
    name: 'Tiago Assunção',
    email: 'tiago.assuncao@oficina.example',
    phone: '11987650000',
    role: 'ATTENDANT',
    hiredAt: '2022-01-10',
    registration: null,
    terminatedAt: '2026-05-29',
    active: false,
  },
];

/* --------------------------------------------------------------------------
   Customers and vehicles
   -------------------------------------------------------------------------- */

export const DEMO_CUSTOMERS: Customer[] = [
  { id: 'c-001', name: 'Marina Albuquerque', email: 'marina.alb@example.com', document: '384.207.115-60', documentType: 'CPF', phone: '(11) 98214-7730', active: true, createdAt: '2024-03-18' },
  { id: 'c-002', name: 'Otávio Bastos', email: 'otavio.bastos@example.com', document: '229.884.370-11', documentType: 'CPF', phone: '(11) 99652-1184', active: true, createdAt: '2023-11-02' },
  { id: 'c-003', name: 'Transportes Vale Verde LTDA', email: 'frota@valeverde.example', document: '18.442.907/0001-35', documentType: 'CNPJ', phone: '(11) 3387-2200', active: true, createdAt: '2022-07-25' },
  { id: 'c-004', name: 'Juliana Kowalski', email: 'ju.kowalski@example.com', document: '551.330.298-04', documentType: 'CPF', phone: '(11) 97418-3320', active: true, createdAt: '2025-01-14' },
  { id: 'c-005', name: 'Fernando Sequeira', email: 'f.sequeira@example.com', document: '702.115.446-83', documentType: 'CPF', phone: '(11) 98890-5512', active: true, createdAt: '2024-09-06' },
  { id: 'c-006', name: 'Priscila Tavares', email: 'pri.tavares@example.com', document: '146.923.507-22', documentType: 'CPF', phone: '(11) 99105-8847', active: false, createdAt: '2023-05-30' },
  { id: 'c-007', name: 'Gustavo Rennó', email: 'gustavo.renno@example.com', document: '830.664.209-77', documentType: 'CPF', phone: '(11) 98337-2091', active: true, createdAt: '2025-06-11' },
  { id: 'c-008', name: 'Adriana Peçanha', email: 'a.pecanha@example.com', document: '415.778.320-58', documentType: 'CPF', phone: '(11) 99728-4416', active: true, createdAt: '2024-12-03' },
  { id: 'c-009', name: 'Leonardo Vasques', email: 'leo.vasques@example.com', document: '967.204.881-19', documentType: 'CPF', phone: '(11) 98561-7734', active: true, createdAt: '2023-08-21' },
  { id: 'c-010', name: 'Camila Ostrowski', email: 'c.ostrowski@example.com', document: '273.590.164-06', documentType: 'CPF', phone: '(11) 99483-1157', active: true, createdAt: '2025-04-02' },
];

export const DEMO_VEHICLES: Vehicle[] = [
  { id: 'v-001', customerId: 'c-001', customerName: 'Marina Albuquerque', licensePlate: 'RJP7A41', make: 'Volkswagen', model: 'Gol 1.6 MSI', modelYear: 2019, manufactureYear: 2018, color: 'Silver', vehicleType: 'CAR', fuelType: 'FLEX', transmissionType: 'MANUAL', active: true },
  { id: 'v-002', customerId: 'c-001', customerName: 'Marina Albuquerque', licensePlate: 'FQT2B88', make: 'Honda', model: 'HR-V EXL', modelYear: 2022, manufactureYear: 2022, color: 'White', vehicleType: 'SUV', fuelType: 'FLEX', transmissionType: 'CVT', active: true },
  { id: 'v-003', customerId: 'c-002', customerName: 'Otávio Bastos', licensePlate: 'GKD5J09', make: 'Toyota', model: 'Corolla XEI', modelYear: 2021, manufactureYear: 2020, color: 'Black', vehicleType: 'CAR', fuelType: 'FLEX', transmissionType: 'AUTOMATIC', active: true },
  { id: 'v-004', customerId: 'c-003', customerName: 'Transportes Vale Verde LTDA', licensePlate: 'MWC4R62', make: 'Mercedes-Benz', model: 'Sprinter 415', modelYear: 2020, manufactureYear: 2019, color: 'White', vehicleType: 'VAN', fuelType: 'DIESEL', transmissionType: 'MANUAL', active: true },
  { id: 'v-005', customerId: 'c-003', customerName: 'Transportes Vale Verde LTDA', licensePlate: 'MWC4R63', make: 'Mercedes-Benz', model: 'Sprinter 415', modelYear: 2020, manufactureYear: 2019, color: 'White', vehicleType: 'VAN', fuelType: 'DIESEL', transmissionType: 'MANUAL', active: true },
  { id: 'v-006', customerId: 'c-003', customerName: 'Transportes Vale Verde LTDA', licensePlate: 'PBH8N15', make: 'Iveco', model: 'Daily 35-150', modelYear: 2018, manufactureYear: 2018, color: 'White', vehicleType: 'TRUCK', fuelType: 'DIESEL', transmissionType: 'MANUAL', active: true },
  { id: 'v-007', customerId: 'c-004', customerName: 'Juliana Kowalski', licensePlate: 'SDA9C27', make: 'Hyundai', model: 'HB20 Comfort', modelYear: 2023, manufactureYear: 2023, color: 'Red', vehicleType: 'CAR', fuelType: 'FLEX', transmissionType: 'MANUAL', active: true },
  { id: 'v-008', customerId: 'c-005', customerName: 'Fernando Sequeira', licensePlate: 'LTU3F70', make: 'Jeep', model: 'Renegade Longitude', modelYear: 2021, manufactureYear: 2021, color: 'Grey', vehicleType: 'SUV', fuelType: 'FLEX', transmissionType: 'AUTOMATIC', active: true },
  { id: 'v-009', customerId: 'c-005', customerName: 'Fernando Sequeira', licensePlate: 'BXE1K54', make: 'Yamaha', model: 'MT-03', modelYear: 2020, manufactureYear: 2020, color: 'Blue', vehicleType: 'MOTORCYCLE', fuelType: 'GASOLINE', transmissionType: 'MANUAL', active: true },
  { id: 'v-010', customerId: 'c-006', customerName: 'Priscila Tavares', licensePlate: 'NUR6D83', make: 'Fiat', model: 'Argo Drive', modelYear: 2020, manufactureYear: 2019, color: 'White', vehicleType: 'CAR', fuelType: 'FLEX', transmissionType: 'MANUAL', active: true },
  { id: 'v-011', customerId: 'c-007', customerName: 'Gustavo Rennó', licensePlate: 'HZO4V19', make: 'Chevrolet', model: 'Onix LTZ', modelYear: 2022, manufactureYear: 2022, color: 'Black', vehicleType: 'CAR', fuelType: 'FLEX', transmissionType: 'AUTOMATIC', active: true },
  { id: 'v-012', customerId: 'c-008', customerName: 'Adriana Peçanha', licensePlate: 'QVI2S66', make: 'Renault', model: 'Kwid Zen', modelYear: 2021, manufactureYear: 2021, color: 'Orange', vehicleType: 'CAR', fuelType: 'FLEX', transmissionType: 'MANUAL', active: true },
  { id: 'v-013', customerId: 'c-009', customerName: 'Leonardo Vasques', licensePlate: 'JAF7T35', make: 'Ford', model: 'Ranger XLS 3.2', modelYear: 2019, manufactureYear: 2019, color: 'Silver', vehicleType: 'TRUCK', fuelType: 'DIESEL', transmissionType: 'AUTOMATIC', active: true },
  { id: 'v-014', customerId: 'c-009', customerName: 'Leonardo Vasques', licensePlate: 'CEM5G02', make: 'Volkswagen', model: 'T-Cross Highline', modelYear: 2023, manufactureYear: 2023, color: 'Blue', vehicleType: 'SUV', fuelType: 'FLEX', transmissionType: 'AUTOMATIC', active: true },
  { id: 'v-015', customerId: 'c-010', customerName: 'Camila Ostrowski', licensePlate: 'ROB8L48', make: 'Nissan', model: 'Kicks SV', modelYear: 2022, manufactureYear: 2021, color: 'White', vehicleType: 'SUV', fuelType: 'FLEX', transmissionType: 'CVT', active: true },
];

/* --------------------------------------------------------------------------
   Inventory
   -------------------------------------------------------------------------- */

export const DEMO_PARTS: Part[] = [
  { id: 'p-001', sku: 'BRK-PAD-0142', name: 'Front brake pad set', description: 'Ceramic, ventilated disc', brand: 'Bosch', unitOfMeasure: 'SET', salePrice: 289.9, averageCost: 172.4, quantityOnHand: 14, quantityReserved: 4, available: 10, stockStatus: 'OK', active: true, createdAt: '2023-01-12', updatedAt: day(-3) },
  { id: 'p-002', sku: 'BRK-DSC-0088', name: 'Front brake disc', description: 'Ventilated, 283mm', brand: 'Fremax', unitOfMeasure: 'UNIT', salePrice: 341.0, averageCost: 208.15, quantityOnHand: 6, quantityReserved: 2, available: 4, stockStatus: 'LOW', active: true, createdAt: '2023-01-12', updatedAt: day(-9) },
  { id: 'p-003', sku: 'FLT-OIL-0311', name: 'Oil filter', description: 'Spin-on', brand: 'Mann', unitOfMeasure: 'UNIT', salePrice: 47.5, averageCost: 22.8, quantityOnHand: 62, quantityReserved: 6, available: 56, stockStatus: 'OK', active: true, createdAt: '2022-08-04', updatedAt: day(-1) },
  { id: 'p-004', sku: 'FLT-AIR-0207', name: 'Engine air filter', description: null, brand: 'Tecfil', unitOfMeasure: 'UNIT', salePrice: 68.9, averageCost: 31.2, quantityOnHand: 38, quantityReserved: 3, available: 35, stockStatus: 'OK', active: true, createdAt: '2022-08-04', updatedAt: day(-1) },
  { id: 'p-005', sku: 'LUB-SYN-5W30', name: 'Synthetic engine oil 5W30', description: 'API SP', brand: 'Motul', unitOfMeasure: 'LITER', salePrice: 79.9, averageCost: 41.6, quantityOnHand: 84, quantityReserved: 12, available: 72, stockStatus: 'OK', active: true, createdAt: '2022-08-04', updatedAt: day(-1) },
  { id: 'p-006', sku: 'IGN-SPK-0455', name: 'Iridium spark plug', description: null, brand: 'NGK', unitOfMeasure: 'UNIT', salePrice: 96.4, averageCost: 54.9, quantityOnHand: 3, quantityReserved: 8, available: -5, stockStatus: 'OUT', active: true, createdAt: '2022-11-19', updatedAt: day(0, 11) },
  { id: 'p-007', sku: 'SUS-SHK-0921', name: 'Rear shock absorber', description: 'Gas-pressurised', brand: 'Cofap', unitOfMeasure: 'UNIT', salePrice: 412.0, averageCost: 246.7, quantityOnHand: 8, quantityReserved: 0, available: 8, stockStatus: 'OK', active: true, createdAt: '2023-03-27', updatedAt: day(-14) },
  { id: 'p-008', sku: 'ELE-BAT-0060', name: 'Battery 60Ah', description: 'Sealed, 12V', brand: 'Moura', unitOfMeasure: 'UNIT', salePrice: 649.0, averageCost: 398.0, quantityOnHand: 5, quantityReserved: 1, available: 4, stockStatus: 'LOW', active: true, createdAt: '2022-08-04', updatedAt: day(-6) },
  { id: 'p-009', sku: 'TRN-CLT-0338', name: 'Clutch kit', description: 'Disc, plate and bearing', brand: 'Luk', unitOfMeasure: 'SET', salePrice: 1284.0, averageCost: 812.5, quantityOnHand: 2, quantityReserved: 1, available: 1, stockStatus: 'LOW', active: true, createdAt: '2023-06-15', updatedAt: day(-21) },
  { id: 'p-010', sku: 'COL-RAD-0512', name: 'Radiator', description: 'Aluminium core', brand: 'Valeo', unitOfMeasure: 'UNIT', salePrice: 878.0, averageCost: 545.3, quantityOnHand: 1, quantityReserved: 2, available: -1, stockStatus: 'OUT', active: true, createdAt: '2023-09-08', updatedAt: day(0, 9) },
  { id: 'p-011', sku: 'ENG-BLT-0173', name: 'Timing belt kit', description: 'Belt, tensioner and idler', brand: 'Gates', unitOfMeasure: 'SET', salePrice: 736.0, averageCost: 449.9, quantityOnHand: 4, quantityReserved: 1, available: 3, stockStatus: 'LOW', active: true, createdAt: '2023-02-02', updatedAt: day(-11) },
  { id: 'p-012', sku: 'ELE-ALT-0244', name: 'Alternator 90A', description: 'Remanufactured', brand: 'Denso', unitOfMeasure: 'UNIT', salePrice: 1156.0, averageCost: 702.0, quantityOnHand: 0, quantityReserved: 0, available: 0, stockStatus: 'OUT', active: true, createdAt: '2024-01-30', updatedAt: day(-30) },
  { id: 'p-013', sku: 'FLT-CAB-0119', name: 'Cabin air filter', description: 'Activated carbon', brand: 'Mann', unitOfMeasure: 'UNIT', salePrice: 89.9, averageCost: 42.1, quantityOnHand: 27, quantityReserved: 2, available: 25, stockStatus: 'OK', active: true, createdAt: '2022-08-04', updatedAt: day(-2) },
  { id: 'p-014', sku: 'BRK-FLD-DOT4', name: 'Brake fluid DOT 4', description: null, brand: 'Bosch', unitOfMeasure: 'LITER', salePrice: 54.0, averageCost: 24.9, quantityOnHand: 19, quantityReserved: 2, available: 17, stockStatus: 'OK', active: true, createdAt: '2022-08-04', updatedAt: day(-4) },
  { id: 'p-015', sku: 'SUS-ARM-0677', name: 'Lower control arm', description: 'With bushings', brand: 'Nakata', unitOfMeasure: 'UNIT', salePrice: 528.0, averageCost: 318.4, quantityOnHand: 3, quantityReserved: 0, available: 3, stockStatus: 'LOW', active: false, createdAt: '2023-05-19', updatedAt: day(-60) },
];

export const DEMO_SERVICES: RepairService[] = [
  { id: 's-001', code: 'SVC-001', name: 'Front brake pad replacement', description: 'Both sides, includes bedding-in', price: 240.0, executionMinutes: 74, estimated: false, executionCount: 12, active: true },
  { id: 's-002', code: 'SVC-002', name: 'Engine oil and filter change', description: null, price: 130.0, executionMinutes: 38, estimated: false, executionCount: 12, active: true },
  { id: 's-003', code: 'SVC-003', name: 'Full diagnostic inspection', description: 'Scanner plus 32-point visual', price: 180.0, executionMinutes: 55, estimated: false, executionCount: 12, active: true },
  { id: 's-004', code: 'SVC-004', name: 'Timing belt replacement', description: 'Includes tensioner and water pump check', price: 980.0, executionMinutes: 268, estimated: false, executionCount: 12, active: true },
  { id: 's-005', code: 'SVC-005', name: 'Clutch replacement', description: null, price: 1150.0, executionMinutes: 340, estimated: true, executionCount: 0, active: true },
  { id: 's-006', code: 'SVC-006', name: 'Suspension overhaul, rear axle', description: null, price: 640.0, executionMinutes: 186, estimated: false, executionCount: 12, active: true },
  { id: 's-007', code: 'SVC-007', name: 'Air conditioning service', description: 'Regas, leak test, cabin filter', price: 310.0, executionMinutes: 92, estimated: false, executionCount: 12, active: true },
  { id: 's-008', code: 'SVC-008', name: 'Battery test and replacement', description: null, price: 90.0, executionMinutes: 22, estimated: false, executionCount: 12, active: true },
  { id: 's-009', code: 'SVC-009', name: 'Radiator replacement', description: 'Includes coolant flush', price: 520.0, executionMinutes: 154, estimated: true, executionCount: 0, active: true },
  { id: 's-010', code: 'SVC-010', name: 'Wheel alignment and balancing', description: null, price: 190.0, executionMinutes: 48, estimated: false, executionCount: 12, active: true },
];

export const DEMO_VENDORS: Vendor[] = [
  { id: 'vd-001', name: 'Distribuidora Paulista de Autopeças', email: 'pedidos@dpa.example', active: true },
  { id: 'vd-002', name: 'Rolamentos & Freios Zona Sul', email: 'vendas@rfzs.example', active: true },
  { id: 'vd-003', name: 'Lubrificantes Interlagos', email: 'comercial@lubint.example', active: true },
  { id: 'vd-004', name: 'Eletro Auto Mogi', email: 'atendimento@eletroauto.example', active: false },
];

export const DEMO_PURCHASE_ORDERS: PurchaseOrder[] = [
  {
    id: 'po-001', code: 'PO-2026-0148', vendorId: 'vd-002', vendorName: 'Rolamentos & Freios Zona Sul',
    status: 'PLACED', placedAt: day(-2, 10), expectedAt: day(2, 10),
    lines: [
      { id: 'pol-001', partId: 'p-006', quantity: 24, received: 0, unitCost: 54.9 },
      { id: 'pol-002', partId: 'p-001', quantity: 10, received: 0, unitCost: 172.4 },
    ],
  },
  {
    id: 'po-002', code: 'PO-2026-0147', vendorId: 'vd-001', vendorName: 'Distribuidora Paulista de Autopeças',
    status: 'PARTIALLY_RECEIVED', placedAt: day(-8, 14), expectedAt: day(-1, 14),
    lines: [
      { id: 'pol-003', partId: 'p-010', quantity: 4, received: 1, unitCost: 545.3 },
      { id: 'pol-004', partId: 'p-012', quantity: 2, received: 0, unitCost: 702.0 },
    ],
  },
  {
    id: 'po-003', code: 'PO-2026-0146', vendorId: 'vd-003', vendorName: 'Lubrificantes Interlagos',
    status: 'RECEIVED', placedAt: day(-16, 9), expectedAt: day(-12, 9),
    lines: [{ id: 'pol-005', partId: 'p-005', quantity: 60, received: 60, unitCost: 41.6 }],
  },
  {
    id: 'po-004', code: 'PO-2026-0145', vendorId: 'vd-004', vendorName: 'Eletro Auto Mogi',
    status: 'CANCELLED', placedAt: day(-24, 11), expectedAt: null,
    lines: [{ id: 'pol-006', partId: 'p-008', quantity: 8, received: 0, unitCost: 398.0 }],
  },
];

export const DEMO_REORDER_RULES: ReorderRule[] = [
  { id: 'rr-001', partId: 'p-001', vendorId: 'vd-001', min: 8, max: 24, enabled: true },
  { id: 'rr-002', partId: 'p-003', vendorId: 'vd-001', min: 20, max: 80, enabled: true },
  { id: 'rr-003', partId: 'p-005', vendorId: 'vd-001', min: 40, max: 120, enabled: true },
  { id: 'rr-004', partId: 'p-006', vendorId: 'vd-001', min: 12, max: 48, enabled: true },
  { id: 'rr-005', partId: 'p-010', vendorId: 'vd-001', min: 2, max: 6, enabled: true },
  { id: 'rr-006', partId: 'p-008', vendorId: 'vd-001', min: 4, max: 12, enabled: false },
];

export const DEMO_STOCK_MOVEMENTS: StockMovement[] = [
  { id: 'sm-001', partId: 'p-006', type: 'CONSUMPTION', quantity: -4, unitCost: null, referenceId: 'wo-004', referenceLabel: 'WO-2026-0731', reason: null, occurredAt: day(0, 11) },
  { id: 'sm-002', partId: 'p-005', type: 'PURCHASE', quantity: 60, unitCost: 41.6, referenceId: 'po-003', referenceLabel: 'PO-2026-0146', reason: null, occurredAt: day(-12, 9) },
  { id: 'sm-003', partId: 'p-010', type: 'PURCHASE', quantity: 1, unitCost: 545.3, referenceId: 'po-002', referenceLabel: 'PO-2026-0147', reason: null, occurredAt: day(-1, 14) },
  { id: 'sm-004', partId: 'p-001', type: 'ADJUSTMENT', quantity: -2, unitCost: null, referenceId: null, referenceLabel: null, reason: 'Physical count, two sets damaged in storage', occurredAt: day(-3, 16) },
  { id: 'sm-005', partId: 'p-003', type: 'CONSUMPTION', quantity: -1, unitCost: null, referenceId: 'wo-002', referenceLabel: 'WO-2026-0729', reason: null, occurredAt: day(-1, 15) },
];

/* --------------------------------------------------------------------------
   Work orders — twenty live jobs spread across every status
   -------------------------------------------------------------------------- */

const wo = (
  id: string,
  code: string,
  status: WorkOrder['status'],
  customerId: string,
  vehicleId: string,
  complaint: string,
  mechanicId: string | null,
  createdDaysAgo: number,
  extra: Partial<WorkOrder> = {},
): WorkOrder => {
  const customer = DEMO_CUSTOMERS.find((c) => c.id === customerId)!;
  const vehicle = DEMO_VEHICLES.find((v) => v.id === vehicleId)!;
  const mechanic = mechanicId ? DEMO_WORKERS.find((w) => w.id === mechanicId)! : null;
  return {
    id,
    orderCode: code,
    status,
    customerId,
    customerName: customer.name,
    vehicleId,
    vehicleLabel: `${vehicle.make} ${vehicle.model} ${vehicle.modelYear}`,
    vehiclePlate: vehicle.licensePlate,
    assignedMechanicId: mechanicId,
    assignedMechanicName: mechanic?.name ?? null,
    customerComplaint: complaint,
    diagnosis: null,
    refusalReason: null,
    budgetId: null,
    createdAt: day(-createdDaysAgo, 8, 30),
    updatedAt: day(-createdDaysAgo + 1, 10),
    diagnosticRequestedAt: null,
    diagnosticStartedAt: null,
    diagnosticFinishedAt: null,
    approvedAt: null,
    refusedAt: null,
    serviceStartedAt: null,
    finishedAt: null,
    pickupReadyAt: null,
    deliveredAt: null,
    ...extra,
  };
};

export const DEMO_WORK_ORDERS: WorkOrder[] = [
  wo('wo-001', 'WO-2026-0728', 'RECEIVED', 'c-001', 'v-001', 'Squealing from the front wheels under braking, worse when cold.', null, 0),
  wo('wo-002', 'WO-2026-0729', 'WAITING_DIAGNOSTICS', 'c-004', 'v-007', 'Service light came on yesterday, car feels normal otherwise.', null, 0, {
    diagnosticRequestedAt: day(0, 9, 15),
  }),
  wo('wo-003', 'WO-2026-0730', 'IN_DIAGNOSTICS', 'c-002', 'v-003', 'Rattle from under the car over speed bumps.', 'w-002', 1, {
    diagnosticRequestedAt: day(-1, 9), diagnosticStartedAt: day(0, 8, 45),
  }),
  wo('wo-004', 'WO-2026-0731', 'IN_PROGRESS', 'c-005', 'v-008', 'Misfiring at idle, engine light flashing.', 'w-005', 4, {
    diagnosticRequestedAt: day(-4, 9), diagnosticStartedAt: day(-4, 10), diagnosticFinishedAt: day(-4, 12),
    diagnosis: 'Cylinders 2 and 4 misfiring. Plugs heavily fouled, coil pack resistance within spec. Recommend full plug set and induction clean.',
    budgetId: 'b-004', approvedAt: day(-2, 16, 20), serviceStartedAt: day(0, 11),
  }),
  wo('wo-005', 'WO-2026-0732', 'BUDGET_IN_DRAFT', 'c-009', 'v-013', 'Clutch slipping in third and fourth under load.', 'w-002', 2, {
    diagnosticRequestedAt: day(-2, 9), diagnosticStartedAt: day(-2, 11), diagnosticFinishedAt: day(-1, 15),
    diagnosis: 'Clutch friction material at 12%. Flywheel scored but serviceable. Release bearing noisy.',
    budgetId: 'b-005',
  }),
  wo('wo-006', 'WO-2026-0733', 'WAITING_APPROVAL', 'c-010', 'v-015', 'Air conditioning blows warm after twenty minutes.', 'w-006', 3, {
    diagnosticRequestedAt: day(-3, 9), diagnosticStartedAt: day(-3, 13), diagnosticFinishedAt: day(-2, 10),
    diagnosis: 'Refrigerant at 40% charge, slow leak at the condenser union. Cabin filter saturated.',
    budgetId: 'b-006',
  }),
  wo('wo-007', 'WO-2026-0734', 'WAITING_APPROVAL', 'c-003', 'v-004', 'Fleet vehicle: scheduled 60k service plus brake inspection.', 'w-005', 5, {
    diagnosticRequestedAt: day(-5, 9), diagnosticStartedAt: day(-5, 14), diagnosticFinishedAt: day(-4, 11),
    diagnosis: 'Front discs below minimum thickness. Timing belt due by service interval. Otherwise sound.',
    budgetId: 'b-007',
  }),
  wo('wo-008', 'WO-2026-0735', 'APPROVED', 'c-007', 'v-011', 'Battery flat twice this week after standing overnight.', 'w-006', 6, {
    diagnosticRequestedAt: day(-6, 9), diagnosticStartedAt: day(-6, 11), diagnosticFinishedAt: day(-5, 9),
    diagnosis: 'Battery failed load test at 58% CCA. No parasitic drain found.',
    budgetId: 'b-008', approvedAt: day(-1, 18, 40),
  }),
  wo('wo-009', 'WO-2026-0736', 'IN_PROGRESS', 'c-008', 'v-012', 'Grinding from the rear when reversing.', 'w-002', 7, {
    diagnosticRequestedAt: day(-7, 9), diagnosticStartedAt: day(-7, 10), diagnosticFinishedAt: day(-6, 16),
    diagnosis: 'Rear shock absorbers leaking, both sides. Bump stops degraded.',
    budgetId: 'b-009', approvedAt: day(-4, 9, 10), serviceStartedAt: day(-2, 8, 30),
  }),
  wo('wo-010', 'WO-2026-0737', 'FINISHED', 'c-001', 'v-002', 'Annual service and cabin filter.', 'w-006', 9, {
    diagnosticRequestedAt: day(-9, 9), diagnosticStartedAt: day(-9, 10), diagnosticFinishedAt: day(-9, 11),
    diagnosis: 'Routine service. No faults found beyond a saturated cabin filter.',
    budgetId: 'b-010', approvedAt: day(-8, 12), serviceStartedAt: day(-6, 9), finishedAt: day(0, 12, 40),
  }),
  wo('wo-011', 'WO-2026-0738', 'WAITING_PICKUP', 'c-002', 'v-003', 'Wheel alignment after kerb strike.', 'w-005', 11, {
    diagnosticRequestedAt: day(-11, 9), diagnosticStartedAt: day(-11, 10), diagnosticFinishedAt: day(-11, 11),
    diagnosis: 'Front toe out of specification. No structural damage.',
    budgetId: 'b-011', approvedAt: day(-10, 9), serviceStartedAt: day(-9, 9), finishedAt: day(-8, 14), pickupReadyAt: day(-1, 9, 20),
  }),
  wo('wo-012', 'WO-2026-0739', 'WAITING_PICKUP', 'c-009', 'v-014', 'Front brake pads and discs.', 'w-002', 12, {
    diagnosticRequestedAt: day(-12, 9), diagnosticStartedAt: day(-12, 11), diagnosticFinishedAt: day(-12, 13),
    diagnosis: 'Pads at 2mm, discs lipped beyond service limit.',
    budgetId: 'b-012', approvedAt: day(-11, 10), serviceStartedAt: day(-10, 8), finishedAt: day(-9, 16), pickupReadyAt: day(-3, 10),
  }),
  wo('wo-013', 'WO-2026-0740', 'REFUSED', 'c-006', 'v-010', 'Overheating in traffic, temperature gauge climbing.', 'w-005', 14, {
    diagnosticRequestedAt: day(-14, 9), diagnosticStartedAt: day(-14, 10), diagnosticFinishedAt: day(-13, 15),
    diagnosis: 'Radiator core corroded through at the lower tank. Replacement required.',
    budgetId: 'b-013', refusedAt: day(-11, 19, 5),
    refusalReason: 'Customer will source the radiator themselves and return.',
  }),
  wo('wo-014', 'WO-2026-0741', 'DELIVERED', 'c-005', 'v-009', 'Chain and sprocket wear, noisy transmission.', 'w-006', 19, {
    diagnosticRequestedAt: day(-19, 9), diagnosticStartedAt: day(-19, 10), diagnosticFinishedAt: day(-19, 12),
    diagnosis: 'Chain elongation past limit. Sprockets hooked.',
    budgetId: 'b-014', approvedAt: day(-18, 11), serviceStartedAt: day(-17, 9), finishedAt: day(-16, 15), pickupReadyAt: day(-16, 16), deliveredAt: day(-15, 10, 30),
  }),
  wo('wo-015', 'WO-2026-0742', 'RECEIVED', 'c-003', 'v-005', 'Fleet vehicle: brake warning light intermittent.', null, 0),
  wo('wo-016', 'WO-2026-0743', 'WAITING_DIAGNOSTICS', 'c-003', 'v-006', 'Loss of power on inclines, black smoke.', null, 1, {
    diagnosticRequestedAt: day(-1, 16, 40),
  }),
  wo('wo-017', 'WO-2026-0744', 'IN_DIAGNOSTICS', 'c-007', 'v-011', 'Steering wheel vibrates above 90 km/h.', 'w-006', 2, {
    diagnosticRequestedAt: day(-2, 9), diagnosticStartedAt: day(0, 10, 15),
  }),
  wo('wo-018', 'WO-2026-0745', 'BUDGET_IN_DRAFT', 'c-004', 'v-007', 'Coolant smell in the cabin after long drives.', 'w-002', 3, {
    diagnosticRequestedAt: day(-3, 9), diagnosticStartedAt: day(-3, 14), diagnosticFinishedAt: day(0, 9, 30),
    diagnosis: 'Heater matrix union weeping. No visible pooling yet, but the pressure test drops 0.4 bar in ten minutes.',
    budgetId: 'b-018',
  }),
  wo('wo-019', 'WO-2026-0746', 'APPROVED', 'c-010', 'v-015', 'Timing belt due at 90,000 km.', 'w-005', 4, {
    diagnosticRequestedAt: day(-4, 9), diagnosticStartedAt: day(-4, 15), diagnosticFinishedAt: day(-3, 10),
    diagnosis: 'Belt within interval but showing surface cracking. Tensioner has lateral play.',
    budgetId: 'b-019', approvedAt: day(0, 8, 5),
  }),
  wo('wo-020', 'WO-2026-0747', 'FINISHED', 'c-008', 'v-012', 'Oil change and general check before a long trip.', 'w-006', 5, {
    diagnosticRequestedAt: day(-5, 9), diagnosticStartedAt: day(-5, 10), diagnosticFinishedAt: day(-5, 10, 40),
    diagnosis: 'Routine. Tyres at 4mm, advised replacement within 5,000 km.',
    budgetId: 'b-020', approvedAt: day(-4, 14), serviceStartedAt: day(-1, 9), finishedAt: day(0, 13, 15),
  }),
];

/* --------------------------------------------------------------------------
   Budgets
   -------------------------------------------------------------------------- */

const line = (
  id: string,
  type: 'PART' | 'SERVICE',
  description: string,
  quantity: number,
  unitPrice: number,
  ref: { partId?: string; serviceId?: string },
  timing: { startedAt?: string; finishedAt?: string } = {},
) => ({
  id,
  type,
  description,
  quantity,
  unitPrice,
  lineTotal: Number((quantity * unitPrice).toFixed(2)),
  partId: ref.partId ?? null,
  serviceId: ref.serviceId ?? null,
  startedAt: timing.startedAt ?? null,
  finishedAt: timing.finishedAt ?? null,
});

const budget = (
  id: string,
  workOrderId: string,
  status: Budget['status'],
  lines: Budget['lines'],
  extra: Partial<Budget> = {},
): Budget => ({
  id,
  workOrderId,
  status,
  lines,
  grandTotal: Number(lines.reduce((sum, l) => sum + l.lineTotal, 0).toFixed(2)),
  createdAt: day(-3, 12),
  sentAt: null,
  resolvedAt: null,
  ...extra,
});

export const DEMO_BUDGETS: Budget[] = [
  budget('b-004', 'wo-004', 'APPROVED', [
    line('bl-041', 'SERVICE', 'Full diagnostic inspection', 1, 180.0, { serviceId: 's-003' }, { startedAt: day(-4, 10), finishedAt: day(-4, 12) }),
    line('bl-042', 'PART', 'Iridium spark plug', 4, 96.4, { partId: 'p-006' }),
    line('bl-043', 'SERVICE', 'Engine oil and filter change', 1, 130.0, { serviceId: 's-002' }, { startedAt: day(0, 11, 10) }),
    line('bl-044', 'PART', 'Synthetic engine oil 5W30', 4, 79.9, { partId: 'p-005' }),
    line('bl-045', 'PART', 'Oil filter', 1, 47.5, { partId: 'p-003' }),
  ], { sentAt: day(-3, 9), resolvedAt: day(-2, 16, 20) }),

  budget('b-005', 'wo-005', 'DRAFT', [
    line('bl-051', 'SERVICE', 'Clutch replacement', 1, 1150.0, { serviceId: 's-005' }),
    line('bl-052', 'PART', 'Clutch kit', 1, 1284.0, { partId: 'p-009' }),
  ]),

  budget('b-006', 'wo-006', 'SENT', [
    line('bl-061', 'SERVICE', 'Air conditioning service', 1, 310.0, { serviceId: 's-007' }),
    line('bl-062', 'PART', 'Cabin air filter', 1, 89.9, { partId: 'p-013' }),
  ], { sentAt: day(-2, 11, 30) }),

  budget('b-007', 'wo-007', 'WAITING_SEND', [
    line('bl-071', 'SERVICE', 'Front brake pad replacement', 1, 240.0, { serviceId: 's-001' }),
    line('bl-072', 'PART', 'Front brake pad set', 1, 289.9, { partId: 'p-001' }),
    line('bl-073', 'PART', 'Front brake disc', 2, 341.0, { partId: 'p-002' }),
    line('bl-074', 'SERVICE', 'Timing belt replacement', 1, 980.0, { serviceId: 's-004' }),
    line('bl-075', 'PART', 'Timing belt kit', 1, 736.0, { partId: 'p-011' }),
  ]),

  budget('b-008', 'wo-008', 'APPROVED', [
    line('bl-081', 'SERVICE', 'Battery test and replacement', 1, 90.0, { serviceId: 's-008' }),
    line('bl-082', 'PART', 'Battery 60Ah', 1, 649.0, { partId: 'p-008' }),
  ], { sentAt: day(-3, 10), resolvedAt: day(-1, 18, 40) }),

  budget('b-009', 'wo-009', 'APPROVED', [
    line('bl-091', 'SERVICE', 'Suspension overhaul, rear axle', 1, 640.0, { serviceId: 's-006' }, { startedAt: day(-2, 8, 30), finishedAt: day(-1, 12) }),
    line('bl-092', 'PART', 'Rear shock absorber', 2, 412.0, { partId: 'p-007' }),
  ], { sentAt: day(-5, 9), resolvedAt: day(-4, 9, 10) }),

  budget('b-010', 'wo-010', 'APPROVED', [
    line('bl-101', 'SERVICE', 'Engine oil and filter change', 1, 130.0, { serviceId: 's-002' }, { startedAt: day(-6, 9), finishedAt: day(-6, 9, 40) }),
    line('bl-102', 'PART', 'Synthetic engine oil 5W30', 5, 79.9, { partId: 'p-005' }),
    line('bl-103', 'PART', 'Oil filter', 1, 47.5, { partId: 'p-003' }),
    line('bl-104', 'PART', 'Cabin air filter', 1, 89.9, { partId: 'p-013' }),
  ], { sentAt: day(-9, 12), resolvedAt: day(-8, 12) }),

  budget('b-011', 'wo-011', 'APPROVED', [
    line('bl-111', 'SERVICE', 'Wheel alignment and balancing', 1, 190.0, { serviceId: 's-010' }, { startedAt: day(-9, 9), finishedAt: day(-9, 10) }),
  ], { sentAt: day(-11, 12), resolvedAt: day(-10, 9) }),

  budget('b-012', 'wo-012', 'APPROVED', [
    line('bl-121', 'SERVICE', 'Front brake pad replacement', 1, 240.0, { serviceId: 's-001' }, { startedAt: day(-10, 8), finishedAt: day(-10, 9, 20) }),
    line('bl-122', 'PART', 'Front brake pad set', 1, 289.9, { partId: 'p-001' }),
    line('bl-123', 'PART', 'Front brake disc', 2, 341.0, { partId: 'p-002' }),
    line('bl-124', 'PART', 'Brake fluid DOT 4', 1, 54.0, { partId: 'p-014' }),
  ], { sentAt: day(-12, 14), resolvedAt: day(-11, 10) }),

  budget('b-013', 'wo-013', 'REFUSED', [
    line('bl-131', 'SERVICE', 'Radiator replacement', 1, 520.0, { serviceId: 's-009' }),
    line('bl-132', 'PART', 'Radiator', 1, 878.0, { partId: 'p-010' }),
  ], { sentAt: day(-13, 16), resolvedAt: day(-11, 19, 5) }),

  budget('b-014', 'wo-014', 'APPROVED', [
    line('bl-141', 'SERVICE', 'Full diagnostic inspection', 1, 180.0, { serviceId: 's-003' }, { startedAt: day(-17, 9), finishedAt: day(-17, 10) }),
  ], { sentAt: day(-19, 13), resolvedAt: day(-18, 11) }),

  budget('b-018', 'wo-018', 'DRAFT', [
    line('bl-181', 'SERVICE', 'Full diagnostic inspection', 1, 180.0, { serviceId: 's-003' }),
  ]),

  budget('b-019', 'wo-019', 'APPROVED', [
    line('bl-191', 'SERVICE', 'Timing belt replacement', 1, 980.0, { serviceId: 's-004' }),
    line('bl-192', 'PART', 'Timing belt kit', 1, 736.0, { partId: 'p-011' }),
  ], { sentAt: day(-3, 11), resolvedAt: day(0, 8, 5) }),

  budget('b-020', 'wo-020', 'APPROVED', [
    line('bl-201', 'SERVICE', 'Engine oil and filter change', 1, 130.0, { serviceId: 's-002' }, { startedAt: day(-1, 9), finishedAt: day(-1, 9, 45) }),
    line('bl-202', 'PART', 'Synthetic engine oil 5W30', 4, 79.9, { partId: 'p-005' }),
    line('bl-203', 'PART', 'Oil filter', 1, 47.5, { partId: 'p-003' }),
  ], { sentAt: day(-5, 12), resolvedAt: day(-4, 14) }),
];

/* --------------------------------------------------------------------------
   Shortfalls — the blocked rows on the board (brief §7.1)
   -------------------------------------------------------------------------- */

export const DEMO_BLOCKS: WorkOrderBlock[] = [
  {
    workOrderId: 'wo-019',
    blocked: true,
    shortfalls: [
      { partId: 'p-011', sku: 'ENG-BLT-0173', partName: 'Timing belt kit', required: 1, available: 0, short: 1 },
    ],
  },
  {
    workOrderId: 'wo-008',
    blocked: true,
    shortfalls: [
      { partId: 'p-008', sku: 'ELE-BAT-0060', partName: 'Battery 60Ah', required: 1, available: 0, short: 1 },
    ],
  },
];

/* --------------------------------------------------------------------------
   Scheduling
   -------------------------------------------------------------------------- */

export const DEMO_SETTINGS: SchedulingSettings = {
  openFrom: '08:00',
  openTo: '18:00',
  dropoffCapacityPerSlot: 3,
  pickupCapacityPerSlot: 2,
};

export const DEMO_CLOSURES: Closure[] = [
  { date: '2026-09-07', message: 'Independence Day — shop closed.' },
  { date: '2026-10-12', message: null },
  { date: '2026-11-02', message: 'All Souls’ Day — closed, deliveries resume the following morning.' },
];

export const DEMO_APPOINTMENTS: Appointment[] = [
  { id: 'a-001', type: 'DROPOFF', status: 'SCHEDULED', slotStart: day(0, 16, 0), slotEnd: day(0, 16, 30), customerId: 'c-001', customerName: 'Marina Albuquerque', vehicleId: 'v-001', vehicleLabel: 'Volkswagen Gol 1.6 MSI 2019', vehiclePlate: 'RJP7A41', guestName: null, guestPhone: null, guestEmail: null, guestVehicleMake: null, guestVehicleModel: null, guestVehicleYear: null, complaint: 'Brake squeal, as discussed on the phone.', workOrderId: null, workOrderCode: null, cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: null, createdAt: day(-2, 10), updatedAt: day(-2, 10) },
  { id: 'a-002', type: 'DROPOFF', status: 'SCHEDULED', slotStart: day(0, 16, 30), slotEnd: day(0, 17, 0), customerId: null, customerName: undefined, vehicleId: null, guestName: 'Roberto Mancini', guestPhone: '(11) 98776-2214', guestEmail: 'roberto.mancini@example.com', guestVehicleMake: 'Peugeot', guestVehicleModel: '208 Active', guestVehicleYear: 2019, complaint: 'Clutch pedal feels soft and travels too far.', workOrderId: null, workOrderCode: null, cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: null, createdAt: day(-1, 19, 20), updatedAt: day(-1, 19, 20) },
  { id: 'a-003', type: 'PICKUP', status: 'SCHEDULED', slotStart: day(0, 17, 0), slotEnd: day(0, 17, 30), customerId: 'c-002', customerName: 'Otávio Bastos', vehicleId: 'v-003', vehicleLabel: 'Toyota Corolla XEI 2021', vehiclePlate: 'GKD5J09', guestName: null, guestPhone: null, guestEmail: null, guestVehicleMake: null, guestVehicleModel: null, guestVehicleYear: null, complaint: null, workOrderId: 'wo-011', workOrderCode: 'WO-2026-0738', cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: null, createdAt: day(-1, 9, 40), updatedAt: day(-1, 9, 40) },
  { id: 'a-004', type: 'DROPOFF', status: 'COMPLETED', slotStart: day(0, 8, 0), slotEnd: day(0, 8, 30), customerId: 'c-003', customerName: 'Transportes Vale Verde LTDA', vehicleId: 'v-005', vehicleLabel: 'Mercedes-Benz Sprinter 415 2020', vehiclePlate: 'MWC4R63', guestName: null, guestPhone: null, guestEmail: null, guestVehicleMake: null, guestVehicleModel: null, guestVehicleYear: null, complaint: 'Brake warning light intermittent.', workOrderId: 'wo-015', workOrderCode: 'WO-2026-0742', cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: day(0, 8, 12), createdAt: day(-4, 11), updatedAt: day(0, 8, 12) },
  { id: 'a-005', type: 'DROPOFF', status: 'NO_SHOW', slotStart: day(-1, 14, 0), slotEnd: day(-1, 14, 30), customerId: null, guestName: 'Sandra Beltrão', guestPhone: '(11) 99012-4478', guestEmail: 'sandra.beltrao@example.com', guestVehicleMake: 'Citroën', guestVehicleModel: 'C3 Feel', guestVehicleYear: 2020, vehicleId: null, complaint: 'Noise from the front suspension.', workOrderId: null, workOrderCode: null, cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: null, createdAt: day(-6, 15), updatedAt: day(-1, 14, 31) },
  { id: 'a-006', type: 'DROPOFF', status: 'CANCELLED', slotStart: day(-2, 10, 0), slotEnd: day(-2, 10, 30), customerId: 'c-006', customerName: 'Priscila Tavares', vehicleId: 'v-010', vehicleLabel: 'Fiat Argo Drive 2020', vehiclePlate: 'NUR6D83', guestName: null, guestPhone: null, guestEmail: null, guestVehicleMake: null, guestVehicleModel: null, guestVehicleYear: null, complaint: 'Overheating.', workOrderId: null, workOrderCode: null, cancelReason: 'RESCHEDULED', cancelMessage: null, rescheduledToId: 'a-007', checkedInAt: null, createdAt: day(-5, 9), updatedAt: day(-2, 9, 10) },
  { id: 'a-007', type: 'DROPOFF', status: 'SCHEDULED', slotStart: day(1, 9, 0), slotEnd: day(1, 9, 30), customerId: 'c-006', customerName: 'Priscila Tavares', vehicleId: 'v-010', vehicleLabel: 'Fiat Argo Drive 2020', vehiclePlate: 'NUR6D83', guestName: null, guestPhone: null, guestEmail: null, guestVehicleMake: null, guestVehicleModel: null, guestVehicleYear: null, complaint: 'Overheating.', workOrderId: null, workOrderCode: null, cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: null, createdAt: day(-2, 9, 10), updatedAt: day(-2, 9, 10) },
  { id: 'a-008', type: 'PICKUP', status: 'SCHEDULED', slotStart: day(1, 11, 0), slotEnd: day(1, 11, 30), customerId: 'c-009', customerName: 'Leonardo Vasques', vehicleId: 'v-014', vehicleLabel: 'Volkswagen T-Cross Highline 2023', vehiclePlate: 'CEM5G02', guestName: null, guestPhone: null, guestEmail: null, guestVehicleMake: null, guestVehicleModel: null, guestVehicleYear: null, complaint: null, workOrderId: 'wo-012', workOrderCode: 'WO-2026-0739', cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: null, createdAt: day(-3, 11), updatedAt: day(-3, 11) },
  { id: 'a-009', type: 'DROPOFF', status: 'SCHEDULED', slotStart: day(1, 8, 30), slotEnd: day(1, 9, 0), customerId: 'c-010', customerName: 'Camila Ostrowski', vehicleId: 'v-015', vehicleLabel: 'Nissan Kicks SV 2022', vehiclePlate: 'ROB8L48', guestName: null, guestPhone: null, guestEmail: null, guestVehicleMake: null, guestVehicleModel: null, guestVehicleYear: null, complaint: 'Timing belt service.', workOrderId: null, workOrderCode: null, cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: null, createdAt: day(-4, 16), updatedAt: day(-4, 16) },
  { id: 'a-010', type: 'DROPOFF', status: 'SCHEDULED', slotStart: day(2, 10, 0), slotEnd: day(2, 10, 30), customerId: null, guestName: 'Ana Lúcia Ferrari', guestPhone: '(11) 98455-1129', guestEmail: 'analucia.ferrari@example.com', guestVehicleMake: 'Chevrolet', guestVehicleModel: 'Tracker Premier', guestVehicleYear: 2022, vehicleId: null, complaint: 'Warning light on the dashboard, unclear which.', workOrderId: null, workOrderCode: null, cancelReason: null, cancelMessage: null, rescheduledToId: null, checkedInAt: null, createdAt: day(0, 7, 45), updatedAt: day(0, 7, 45) },
];

/* --------------------------------------------------------------------------
   History
   -------------------------------------------------------------------------- */

export const DEMO_HISTORY: Record<string, HistoryEntry[]> = {
  'wo-004': [
    { id: 'h-0401', aggregateType: 'WORK_ORDER', aggregateId: 'wo-004', eventType: 'WorkOrderCreated', actorName: 'Rafael Nogueira', actorIsSystem: false, actorIsRole: false, occurredAt: day(-4, 8, 30), summary: 'Work order opened at the counter from a checked-in drop-off.' },
    { id: 'h-0402', aggregateType: 'WORK_ORDER', aggregateId: 'wo-004', eventType: 'DiagnosticsRequested', actorName: 'Rafael Nogueira', actorIsSystem: false, actorIsRole: false, occurredAt: day(-4, 9), summary: 'Queued for diagnostics.' },
    { id: 'h-0403', aggregateType: 'WORK_ORDER', aggregateId: 'wo-004', eventType: 'DiagnosticsStarted', actorName: 'Emerson Vidal', actorIsSystem: false, actorIsRole: false, occurredAt: day(-4, 10), summary: 'Emerson Vidal began the inspection.' },
    { id: 'h-0404', aggregateType: 'WORK_ORDER', aggregateId: 'wo-004', eventType: 'DiagnosticsFinished', actorName: 'Emerson Vidal', actorIsSystem: false, actorIsRole: false, occurredAt: day(-4, 12), summary: 'Diagnosis recorded. Budget draft opened with 5 lines.' },
    { id: 'h-0405', aggregateType: 'WORK_ORDER', aggregateId: 'wo-004', eventType: 'BudgetSent', actorName: 'Rafael Nogueira', actorIsSystem: false, actorIsRole: false, occurredAt: day(-3, 9), summary: 'Budget frozen and sent to f.sequeira@example.com. Total R$ 1.148,10.' },
    { id: 'h-0406', aggregateType: 'WORK_ORDER', aggregateId: 'wo-004', eventType: 'BudgetApproved', actorName: 'Fernando Sequeira', actorIsSystem: false, actorIsRole: false, occurredAt: day(-2, 16, 20), summary: 'Customer approved the budget.' },
    { id: 'h-0407', aggregateType: 'WORK_ORDER', aggregateId: 'wo-004', eventType: 'ServiceStarted', actorName: 'Emerson Vidal', actorIsSystem: false, actorIsRole: false, occurredAt: day(0, 11), summary: 'Service started. 4 × IGN-SPK-0455 and 4 L of LUB-SYN-5W30 consumed from stock.' },
  ],
  'wo-013': [
    { id: 'h-1301', aggregateType: 'WORK_ORDER', aggregateId: 'wo-013', eventType: 'WorkOrderCreated', actorName: 'Tiago Assunção', actorIsSystem: false, actorIsRole: false, occurredAt: day(-14, 8, 30), summary: 'Work order opened at the counter.' },
    { id: 'h-1302', aggregateType: 'WORK_ORDER', aggregateId: 'wo-013', eventType: 'DiagnosticsFinished', actorName: 'Emerson Vidal', actorIsSystem: false, actorIsRole: false, occurredAt: day(-13, 15), summary: 'Diagnosis recorded. Budget draft opened with 2 lines.' },
    { id: 'h-1303', aggregateType: 'WORK_ORDER', aggregateId: 'wo-013', eventType: 'BudgetSent', actorName: 'Rafael Nogueira', actorIsSystem: false, actorIsRole: false, occurredAt: day(-13, 16), summary: 'Budget frozen and sent. Total R$ 1.398,00.' },
    { id: 'h-1304', aggregateType: 'WORK_ORDER', aggregateId: 'wo-013', eventType: 'BudgetRefused', actorName: 'Priscila Tavares', actorIsSystem: false, actorIsRole: false, occurredAt: day(-11, 19, 5), summary: 'Customer refused the budget. Work order closed as refused.' },
    { id: 'h-1305', aggregateType: 'WORK_ORDER', aggregateId: 'wo-013', eventType: 'ReservationsReleased', actorName: null, actorIsSystem: true, actorIsRole: false, occurredAt: day(-11, 19, 5), summary: 'Reservations released: 1 × COL-RAD-0512 returned to available stock.' },
  ],
};

/** Fallback timeline for orders without an authored history above. */
export function fallbackHistory(order: WorkOrder): HistoryEntry[] {
  const entries: HistoryEntry[] = [
    { id: `${order.id}-h1`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'WorkOrderCreated', actorName: 'Rafael Nogueira', actorIsSystem: false, actorIsRole: false, occurredAt: order.createdAt, summary: 'Work order opened at the counter.' },
  ];
  if (order.diagnosticRequestedAt) entries.push({ id: `${order.id}-h2`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'DiagnosticsRequested', actorName: 'Rafael Nogueira', actorIsSystem: false, actorIsRole: false, occurredAt: order.diagnosticRequestedAt, summary: 'Queued for diagnostics.' });
  if (order.diagnosticStartedAt) entries.push({ id: `${order.id}-h3`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'DiagnosticsStarted', actorName: order.assignedMechanicName ?? null, actorIsSystem: false, actorIsRole: false, occurredAt: order.diagnosticStartedAt, summary: `${order.assignedMechanicName ?? 'A mechanic'} began the inspection.` });
  if (order.diagnosticFinishedAt) entries.push({ id: `${order.id}-h4`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'DiagnosticsFinished', actorName: order.assignedMechanicName ?? null, actorIsSystem: false, actorIsRole: false, occurredAt: order.diagnosticFinishedAt, summary: 'Diagnosis recorded. Budget draft opened.' });
  if (order.approvedAt) entries.push({ id: `${order.id}-h5`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'BudgetApproved', actorName: order.customerName ?? null, actorIsSystem: false, actorIsRole: false, occurredAt: order.approvedAt, summary: 'Customer approved the budget.' });
  if (order.serviceStartedAt) entries.push({ id: `${order.id}-h6`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'ServiceStarted', actorName: order.assignedMechanicName ?? null, actorIsSystem: false, actorIsRole: false, occurredAt: order.serviceStartedAt, summary: 'Service started. Reserved stock consumed.' });
  if (order.finishedAt) entries.push({ id: `${order.id}-h7`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'ServiceFinished', actorName: order.assignedMechanicName ?? null, actorIsSystem: false, actorIsRole: false, occurredAt: order.finishedAt, summary: 'All approved lines completed.' });
  if (order.pickupReadyAt) entries.push({ id: `${order.id}-h8`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'PickupInvitationSent', actorName: null, actorIsSystem: true, actorIsRole: false, occurredAt: order.pickupReadyAt, summary: 'Pickup booking invitation sent. No appointment exists until the customer picks a slot.' });
  if (order.deliveredAt) entries.push({ id: `${order.id}-h9`, aggregateType: 'WORK_ORDER', aggregateId: order.id, eventType: 'Delivered', actorName: 'Rafael Nogueira', actorIsSystem: false, actorIsRole: false, occurredAt: order.deliveredAt, summary: 'Vehicle collected by the customer.' });
  return entries.reverse();
}

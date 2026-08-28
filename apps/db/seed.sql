-- Demo data for auto_repair_shop.
--
-- This is NOT wired into docker-entrypoint-initdb.d: the schema itself only exists after Flyway
-- has run the migrations under apps/backend/src/main/resources/db/migration, which happens on
-- backend startup, not on Postgres container init. Run this file manually after the backend has
-- come up at least once, e.g.:
--
--   docker compose --profile app up -d postgres
--   docker compose --profile app run --rm backend  # let Flyway migrate, then stop it
--   psql "postgresql://auto_repair_shop_user:secret@localhost:5432/auto_repair_shop" -f apps/db/seed.sql
--
-- All demo accounts use the password: Demo@123
-- (bcrypt hash below is a real, verified hash for that password, cost 10)
--
-- Safe to re-run: every insert is keyed by a fixed UUID and guarded with ON CONFLICT DO NOTHING.

\set demo_password_hash '$2a$10$KVFM31F/Q.aX68W.QsPPA.4XOZIE6REBR9FlXLWpNAsEzoLfrd2EK'

-- ==========================================================================
-- users.users / auth / workers / customers
-- ==========================================================================

INSERT INTO users.users (id, first_name, last_name, email, document_type, document_code, email_verified)
VALUES
    ('00000000-0000-7000-8000-000000000001', 'Carlos', 'Nunes', 'carlos.nunes@autorepair.demo', 'CPF', '11122233344', true),
    ('00000000-0000-7000-8000-000000000002', 'Roberto', 'Silva', 'roberto.silva@autorepair.demo', 'CPF', '22233344455', true),
    ('00000000-0000-7000-8000-000000000003', 'Ana', 'Costa', 'ana.costa@autorepair.demo', 'CPF', '33344455566', true),
    ('00000000-0000-7000-8000-000000000004', 'Fernanda', 'Lima', 'fernanda.lima@autorepair.demo', 'CPF', '44455566677', true),
    ('00000000-0000-7000-8000-000000000005', 'Marcos', 'Oliveira', 'marcos.oliveira@autorepair.demo', 'CPF', '55566677788', true),
    ('00000000-0000-7000-8000-000000000011', 'Joao', 'Pereira', 'joao.pereira@example.com', 'CPF', '66677788899', true),
    ('00000000-0000-7000-8000-000000000012', 'Maria', 'Souza', 'maria.souza@example.com', 'CPF', '77788899900', true),
    ('00000000-0000-7000-8000-000000000013', 'Pedro', 'Santos', 'pedro.santos@example.com', 'CPF', '88899900011', true)
ON CONFLICT (id) DO NOTHING;

INSERT INTO auth.user_auths (user_id, provider, password_hash)
SELECT id, 'LOCAL', :'demo_password_hash'
FROM users.users
WHERE id IN (
    '00000000-0000-7000-8000-000000000001',
    '00000000-0000-7000-8000-000000000002',
    '00000000-0000-7000-8000-000000000003',
    '00000000-0000-7000-8000-000000000004',
    '00000000-0000-7000-8000-000000000005',
    '00000000-0000-7000-8000-000000000011',
    '00000000-0000-7000-8000-000000000012',
    '00000000-0000-7000-8000-000000000013'
)
ON CONFLICT (user_id, provider) DO NOTHING;

INSERT INTO users.workers (user_id, registration, role, hire_date, start_date)
VALUES
    ('00000000-0000-7000-8000-000000000001', 'ARS-000101', 'MANAGER', '2022-01-10', '2022-01-10'),
    ('00000000-0000-7000-8000-000000000002', 'ARS-000102', 'MECHANIC', '2022-03-01', '2022-03-01'),
    ('00000000-0000-7000-8000-000000000003', 'ARS-000103', 'MECHANIC', '2023-06-15', '2023-06-15'),
    ('00000000-0000-7000-8000-000000000004', 'ARS-000104', 'ATTENDANT', '2023-02-01', '2023-02-01'),
    ('00000000-0000-7000-8000-000000000005', 'ARS-000105', 'STOCKIST', '2023-09-01', '2023-09-01')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users.customers (user_id)
VALUES
    ('00000000-0000-7000-8000-000000000011'),
    ('00000000-0000-7000-8000-000000000012'),
    ('00000000-0000-7000-8000-000000000013')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO users.phone_numbers (user_id, type, phone, is_primary)
VALUES
    ('00000000-0000-7000-8000-000000000011', 'MOBILE', '+5511988887777', true),
    ('00000000-0000-7000-8000-000000000012', 'MOBILE', '+5511977776666', true),
    ('00000000-0000-7000-8000-000000000013', 'MOBILE', '+5511966665555', true)
ON CONFLICT (user_id, phone) DO NOTHING;

-- ==========================================================================
-- vehicle.vehicles
-- ==========================================================================

INSERT INTO vehicle.vehicles
    (id, customer_id, vehicle_type, license_plate, make, model, color, model_year, manufacture_year, fuel_type, transmission_type)
VALUES
    ('00000000-0000-7000-8000-000000000021', '00000000-0000-7000-8000-000000000011', 'CAR', 'ABC1D23', 'Volkswagen', 'Gol', 'White', 2020, 2020, 'FLEX', 'MANUAL'),
    ('00000000-0000-7000-8000-000000000022', '00000000-0000-7000-8000-000000000012', 'CAR', 'DEF4E56', 'Toyota', 'Corolla', 'Silver', 2022, 2022, 'FLEX', 'AUTOMATIC'),
    ('00000000-0000-7000-8000-000000000023', '00000000-0000-7000-8000-000000000013', 'MOTORCYCLE', 'GHI7F89', 'Honda', 'CG 160', 'Red', 2021, 2021, 'GASOLINE', 'MANUAL')
ON CONFLICT (id) DO NOTHING;

-- ==========================================================================
-- inventory.parts / repair_services / vendors
-- ==========================================================================

INSERT INTO inventory.parts (id, sku, name, description, brand, unit_of_measure, sale_price)
VALUES
    ('00000000-0000-7000-8000-000000000031', 'OIL-5W30-1L', 'Motor oil 5W30', 'Synthetic motor oil, 1 liter', 'Mobil', 'LITER', 45.00),
    ('00000000-0000-7000-8000-000000000032', 'FILT-OIL-STD', 'Oil filter', 'Standard oil filter', 'Bosch', 'UNIT', 32.50),
    ('00000000-0000-7000-8000-000000000033', 'BRK-PAD-FRT', 'Front brake pads (set)', 'Ceramic front brake pad set', 'TRW', 'SET', 189.90),
    ('00000000-0000-7000-8000-000000000034', 'BATT-60AH', 'Car battery 60Ah', '60Ah automotive battery', 'Moura', 'UNIT', 520.00)
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory.repair_services (id, code, name, description, price, estimated_seconds)
VALUES
    ('00000000-0000-7000-8000-000000000041', 'SVC-OIL-CHANGE', 'Oil change', 'Drain and replace engine oil and filter', 120.00, 1800),
    ('00000000-0000-7000-8000-000000000042', 'SVC-BRAKE-FRT', 'Front brake service', 'Replace front brake pads and inspect discs', 220.00, 3600),
    ('00000000-0000-7000-8000-000000000043', 'SVC-DIAGNOSTIC', 'General diagnostic', 'Full vehicle diagnostic check', 90.00, 2700)
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory.vendors (id, name, contact_email)
VALUES
    ('00000000-0000-7000-8000-000000000051', 'AutoPecas Distribuidora', 'contato@autopecas.demo'),
    ('00000000-0000-7000-8000-000000000052', 'Bosch Parts Brasil', 'vendas@boschparts.demo')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory.stock_movements (id, part_id, type, quantity, reason)
VALUES
    ('00000000-0000-7000-8000-000000000061', '00000000-0000-7000-8000-000000000031', 'ADJUSTMENT', 50, 'Initial stock load'),
    ('00000000-0000-7000-8000-000000000062', '00000000-0000-7000-8000-000000000032', 'ADJUSTMENT', 30, 'Initial stock load'),
    ('00000000-0000-7000-8000-000000000063', '00000000-0000-7000-8000-000000000033', 'ADJUSTMENT', 15, 'Initial stock load'),
    ('00000000-0000-7000-8000-000000000064', '00000000-0000-7000-8000-000000000034', 'ADJUSTMENT', 8, 'Initial stock load')
ON CONFLICT (id) DO NOTHING;

-- ==========================================================================
-- work_orders.orders / budgets / budget_lines
-- ==========================================================================

-- Order 1: fully delivered, with an approved budget
INSERT INTO work_orders.orders
    (id, order_code, status, customer_id, vehicle_id, assigned_mechanic_id,
     customer_name, vehicle_plate, vehicle_make, vehicle_model, mechanic_name,
     customer_complaint, diagnosis, created_at, approved_at, finished_at, delivered_at)
VALUES
    ('00000000-0000-7000-8000-000000000071', 'WO-20260818-000001', 'DELIVERED',
     '00000000-0000-7000-8000-000000000011', '00000000-0000-7000-8000-000000000021', '00000000-0000-7000-8000-000000000002',
     'Joao Pereira', 'ABC1D23', 'Volkswagen', 'Gol', 'Roberto Silva',
     'Engine noise and burning oil smell', 'Oil change and filter replacement overdue',
     now() - interval '10 days', now() - interval '9 days', now() - interval '8 days', now() - interval '7 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO work_orders.budgets (id, work_order_id, status, sent_at, resolved_at)
VALUES
    ('00000000-0000-7000-8000-000000000081', '00000000-0000-7000-8000-000000000071', 'APPROVED', now() - interval '9 days 12 hours', now() - interval '9 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO work_orders.budget_lines (id, budget_id, type, description, quantity, unit_price, part_id, service_id)
VALUES
    ('00000000-0000-7000-8000-000000000091', '00000000-0000-7000-8000-000000000081', 'SERVICE', 'Oil change', 1, 120.00, NULL, '00000000-0000-7000-8000-000000000041'),
    ('00000000-0000-7000-8000-000000000092', '00000000-0000-7000-8000-000000000081', 'PART', 'Motor oil 5W30', 4, 45.00, '00000000-0000-7000-8000-000000000031', NULL),
    ('00000000-0000-7000-8000-000000000093', '00000000-0000-7000-8000-000000000081', 'PART', 'Oil filter', 1, 32.50, '00000000-0000-7000-8000-000000000032', NULL)
ON CONFLICT (id) DO NOTHING;

-- Order 2: in progress, budget already approved
INSERT INTO work_orders.orders
    (id, order_code, status, customer_id, vehicle_id, assigned_mechanic_id,
     customer_name, vehicle_plate, vehicle_make, vehicle_model, mechanic_name,
     customer_complaint, diagnosis, created_at, approved_at)
VALUES
    ('00000000-0000-7000-8000-000000000072', 'WO-20260825-000002', 'IN_PROGRESS',
     '00000000-0000-7000-8000-000000000012', '00000000-0000-7000-8000-000000000022', '00000000-0000-7000-8000-000000000003',
     'Maria Souza', 'DEF4E56', 'Toyota', 'Corolla', 'Ana Costa',
     'Squeaking noise when braking', 'Front brake pads worn down',
     now() - interval '3 days', now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO work_orders.budgets (id, work_order_id, status, sent_at, resolved_at)
VALUES
    ('00000000-0000-7000-8000-000000000082', '00000000-0000-7000-8000-000000000072', 'APPROVED', now() - interval '2 days 6 hours', now() - interval '2 days')
ON CONFLICT (id) DO NOTHING;

INSERT INTO work_orders.budget_lines (id, budget_id, type, description, quantity, unit_price, part_id, service_id)
VALUES
    ('00000000-0000-7000-8000-000000000094', '00000000-0000-7000-8000-000000000082', 'SERVICE', 'Front brake service', 1, 220.00, NULL, '00000000-0000-7000-8000-000000000042'),
    ('00000000-0000-7000-8000-000000000095', '00000000-0000-7000-8000-000000000082', 'PART', 'Front brake pads (set)', 1, 189.90, '00000000-0000-7000-8000-000000000033', NULL)
ON CONFLICT (id) DO NOTHING;

-- Order 3: waiting on customer approval, budget sent
INSERT INTO work_orders.orders
    (id, order_code, status, customer_id, vehicle_id, assigned_mechanic_id,
     customer_name, vehicle_plate, vehicle_make, vehicle_model, mechanic_name,
     customer_complaint, diagnosis, created_at)
VALUES
    ('00000000-0000-7000-8000-000000000073', 'WO-20260827-000003', 'WAITING_APPROVAL',
     '00000000-0000-7000-8000-000000000013', '00000000-0000-7000-8000-000000000023', '00000000-0000-7000-8000-000000000002',
     'Pedro Santos', 'GHI7F89', 'Honda', 'CG 160', 'Roberto Silva',
     'Battery not holding charge', 'Battery near end of life, needs replacement',
     now() - interval '1 day')
ON CONFLICT (id) DO NOTHING;

INSERT INTO work_orders.budgets (id, work_order_id, status, sent_at)
VALUES
    ('00000000-0000-7000-8000-000000000083', '00000000-0000-7000-8000-000000000073', 'SENT', now() - interval '12 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO work_orders.budget_lines (id, budget_id, type, description, quantity, unit_price, part_id, service_id)
VALUES
    ('00000000-0000-7000-8000-000000000096', '00000000-0000-7000-8000-000000000083', 'PART', 'Car battery 60Ah', 1, 520.00, '00000000-0000-7000-8000-000000000034', NULL)
ON CONFLICT (id) DO NOTHING;

-- Order 4: just received, no budget yet
INSERT INTO work_orders.orders
    (id, order_code, status, customer_id, vehicle_id,
     customer_name, vehicle_plate, vehicle_make, vehicle_model,
     customer_complaint, created_at)
VALUES
    ('00000000-0000-7000-8000-000000000074', 'WO-20260828-000004', 'RECEIVED',
     '00000000-0000-7000-8000-000000000011', '00000000-0000-7000-8000-000000000021',
     'Joao Pereira', 'ABC1D23', 'Volkswagen', 'Gol',
     'Air conditioning not cooling', now() - interval '2 hours')
ON CONFLICT (id) DO NOTHING;

INSERT INTO inventory.part_reservations (id, part_id, work_order_id, quantity_requested, quantity_reserved, status)
VALUES
    ('00000000-0000-7000-8000-0000000000a1', '00000000-0000-7000-8000-000000000033', '00000000-0000-7000-8000-000000000072', 1, 1, 'HELD'),
    ('00000000-0000-7000-8000-0000000000a2', '00000000-0000-7000-8000-000000000034', '00000000-0000-7000-8000-000000000073', 1, 0, 'HELD')
ON CONFLICT (id) DO NOTHING;

-- ==========================================================================
-- Keep app-generated codes (registration numbers, order codes) from ever
-- colliding with the hardcoded demo codes above.
-- ==========================================================================

SELECT setval('users.seq_worker_registration', 1000, false)
WHERE (SELECT last_value FROM users.seq_worker_registration) < 1000;

SELECT setval('work_orders.seq_work_order_code', 1000, false)
WHERE (SELECT last_value FROM work_orders.seq_work_order_code) < 1000;
